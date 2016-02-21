package suite.immutable.btree.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.PageFileImpl;
import suite.file.impl.SerializedPageFileImpl;
import suite.fs.KeyDataStoreMutator;
import suite.immutable.btree.IbTree;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Util;

/**
 * Immutable, on-disk B-tree implementation.
 *
 * To allow efficient page management, a large B-tree has one smaller B-tree for
 * storing unused pages, called allocation B-tree. That smaller one might
 * contain a even smaller allocation B-tree, until it becomes small enough to
 * fit in a single disk page.
 *
 * Mutator control is done by a "stamp" consisting of a chain of root page
 * numbers of all B-trees. The holder object persist the stamp into another
 * file.
 *
 * @author ywsing
 */
public class IbTreeImpl<Key> implements IbTree<Key> {

	private String filename;
	private int pageSize;
	private Comparator<Key> comparator;
	private Serializer<Key> serializer;
	private Mutate mutate;

	private PageFile pageFile0;
	private SerializedPageFile<Page> pageFile;
	private SerializedPageFile<Bytes> payloadFile;
	private IbTreeImpl<Integer> allocationIbTree;

	private int maxBranchFactor; // Exclusive
	private int minBranchFactor; // Inclusive

	public static Serializer<Integer> pointerSerializer = Serialize.nullable(Serialize.int_);

	private class Page {
		private List<Slot> slots;

		private Page(List<Slot> slots) {
			this.slots = slots;
		}
	}

	private enum SlotType {
		BRANCH, DATA, TERMINAL,
	}

	/**
	 * In leaves, pointer would be null, and pivot stores the leaf value.
	 *
	 * Pivot would be null at the minimum side of a tree as the guarding key.
	 */
	private class Slot {
		private SlotType type;
		private Key pivot;
		private Integer pointer;

		private Slot(SlotType type, Key pivot, Integer pointer) {
			this.type = type;
			this.pivot = pivot;
			this.pointer = pointer;
		}

		private List<Slot> slots() {
			return type == SlotType.BRANCH ? read(pointer).slots : null;
		}
	}

	private class FindSlot {
		private Slot slot;
		private int i, c;

		private FindSlot(List<Slot> slots, Key key) {
			this(slots, key, true);
		}

		private FindSlot(List<Slot> slots, Key key, boolean isInclusive) {
			for (i = slots.size() - 1; 0 <= i; i--)
				if ((c = comparator.compare((slot = slots.get(i)).pivot, key)) <= 0)
					if (isInclusive || c < 0)
						break;
		}
	}

	private interface Allocator {
		public Integer allocate();

		public void discard(Integer pointer);

		public List<Integer> flush();
	}

	/**
	 * Protect discarded pages belonging to previous mutations, so that they are
	 * not being allocated immediately. This supports immutability (i.e.
	 * copy-on-write) and with this recovery can succeed.
	 *
	 * On the other hand, allocated and discarded pages are reused here, since
	 * they belong to current mutation.
	 */
	private class DelayedDiscardAllocator implements Allocator {
		private Allocator allocator;
		private Set<Integer> allocated = new HashSet<>();
		private Deque<Integer> discarded = new ArrayDeque<>(); // Non-reusable
		private Deque<Integer> allocateDiscarded = new ArrayDeque<>(); // Reusable

		private DelayedDiscardAllocator(Allocator allocator) {
			this.allocator = allocator;
		}

		public Integer allocate() {
			Integer pointer = allocateDiscarded.isEmpty() ? allocator.allocate() : allocateDiscarded.pop();
			allocated.add(pointer);
			return pointer;
		}

		public void discard(Integer pointer) {
			(allocated.remove(pointer) ? allocateDiscarded : discarded).push(pointer);
		}

		public List<Integer> flush() {
			while (!discarded.isEmpty())
				allocator.discard(discarded.pop());
			while (!allocateDiscarded.isEmpty())
				allocator.discard(allocateDiscarded.pop());
			return allocator.flush();
		}
	}

	private class SwappingAllocator implements Allocator {
		private int active;
		private Deque<Integer> deque;

		private SwappingAllocator(int active) {
			reset(active);
		}

		public Integer allocate() {
			return deque.pop();
		}

		public void discard(Integer pointer) {
		}

		public List<Integer> flush() {
			List<Integer> stamp = Arrays.asList(active);
			reset(1 - active);
			return stamp;
		}

		private void reset(int active) {
			this.active = active;
			deque = new ArrayDeque<>(Arrays.asList(1 - active));
		}
	}

	private class SubIbTreeAllocator implements Allocator {
		private IbTreeImpl<Integer>.Mutator mutator;

		private SubIbTreeAllocator(IbTreeImpl<Integer>.Mutator mutator) {
			this.mutator = mutator;
		}

		public Integer allocate() {
			Integer pointer = mutator.keys(0, Integer.MAX_VALUE).first();
			if (pointer != null) {
				mutator.remove(pointer);
				return pointer;
			} else
				throw new RuntimeException("Pages exhausted");
		}

		public void discard(Integer pointer) {
			mutator.putTerminal(pointer);
		}

		public List<Integer> flush() {
			return mutator.flush();
		}
	}

	public class Mutator implements KeyDataStoreMutator<Key> {
		private Allocator allocator;
		private Integer root;

		private Mutator(Allocator allocator) {
			this.allocator = allocator;
			root = persist(Arrays.asList(new Slot(SlotType.TERMINAL, null, null)));
		}

		private Mutator(Allocator allocator, Integer root) {
			this.allocator = allocator;
			this.root = root;
		}

		@Override
		public void end(boolean isComplete) {
			if (isComplete)
				mutate.commit(this);
		}

		@Override
		public Streamlet<Key> keys(Key start, Key end) {
			return IbTreeImpl.this.keys(root, start, end);
		}

		@Override
		public Integer get(Key key) {
			return get0(root, key, SlotType.TERMINAL);
		}

		@Override
		public Bytes getPayload(Key key) {
			Integer pointer = get0(root, key, SlotType.DATA);
			return pointer != null ? payloadFile.load(pointer) : null;
		}

		@Override
		public boolean getTerminal(Key key) {
			return stream(root, key, null).first() != null;
		}

		@Override
		public void put(Key key, Integer data) {
			update(key, new Slot(SlotType.TERMINAL, key, data));
		}

		@Override
		public void putPayload(Key key, Bytes payload) {
			Integer pointer = allocator.allocate();
			payloadFile.save(pointer, payload);
			update(key, new Slot(SlotType.DATA, key, pointer));
		}

		@Override
		public void putTerminal(Key key) {
			update(key, new Slot(SlotType.TERMINAL, key, null));
		}

		@Override
		public void remove(Key key) {
			allocator.discard(root);
			root = createRootPage(delete(read(root).slots, key));
		}

		private void update(Key key, Slot slot1) {
			update(key, slot -> slot1);
		}

		private void update(Key key, Fun<Slot, Slot> fun) {
			allocator.discard(root);
			root = createRootPage(update(read(root).slots, key, fun));
		}

		private List<Slot> update(List<Slot> slots0, Key key, Fun<Slot, Slot> fun) {
			FindSlot fs = new FindSlot(slots0, key);
			int s0 = fs.i;
			int s1 = fs.i + 1;
			List<Slot> replaceSlots;

			// Adds the node into it
			if (fs.slot.type == SlotType.BRANCH)
				replaceSlots = update(discard(fs.slot).slots(), key, fun);
			else if (fs.c != 0)
				replaceSlots = Arrays.asList(fs.slot, fun.apply(null));
			else
				replaceSlots = Arrays.asList(fun.apply(discard(fs.slot)));

			List<Slot> slots1 = Util.add(Util.left(slots0, s0), replaceSlots, Util.right(slots0, s1));
			List<Slot> slots2;

			// Checks if need to split
			if (slots1.size() < maxBranchFactor)
				slots2 = Arrays.asList(slot(slots1));
			else { // Splits into two if reached maximum number of nodes
				List<Slot> leftSlots = Util.left(slots1, minBranchFactor);
				List<Slot> rightSlots = Util.right(slots1, minBranchFactor);
				slots2 = Arrays.asList(slot(leftSlots), slot(rightSlots));
			}

			return slots2;
		}

		private List<Slot> delete(List<Slot> slots0, Key key) {
			FindSlot fs = new FindSlot(slots0, key);
			int size = slots0.size();
			int s0 = fs.i, s1 = fs.i + 1;
			List<Slot> replaceSlots;

			// Removes the node from it
			if (fs.slot.type == SlotType.BRANCH) {
				List<Slot> slots1 = delete(discard(fs.slot).slots(), key);

				// Merges with a neighbor if reached minimum number of nodes
				if (slots1.size() < minBranchFactor)
					if (0 < s0)
						replaceSlots = merge(discard(slots0.get(--s0)).slots(), slots1);
					else if (s1 < size)
						replaceSlots = merge(slots1, discard(slots0.get(s1++)).slots());
					else
						replaceSlots = Arrays.asList(slot(slots1));
				else
					replaceSlots = Arrays.asList(slot(slots1));
			} else if (fs.c == 0)
				replaceSlots = Collections.emptyList();
			else
				throw new RuntimeException("Node not found " + key);

			return Util.add(Util.left(slots0, s0), replaceSlots, Util.right(slots0, s1));
		}

		private List<Slot> merge(List<Slot> slots0, List<Slot> slots1) {
			List<Slot> merged;

			if (maxBranchFactor <= slots0.size() + slots1.size()) {
				List<Slot> leftSlots, rightSlots;

				if (minBranchFactor < slots0.size()) {
					leftSlots = Util.left(slots0, -1);
					rightSlots = Util.add(Arrays.asList(Util.last(slots0)), slots1);
				} else if (minBranchFactor < slots1.size()) {
					leftSlots = Util.add(slots0, Arrays.asList(Util.first(slots1)));
					rightSlots = Util.right(slots1, 1);
				} else {
					leftSlots = slots0;
					rightSlots = slots1;
				}

				merged = Arrays.asList(slot(leftSlots), slot(rightSlots));
			} else
				merged = Arrays.asList(slot(Util.add(slots0, slots1)));

			return merged;
		}

		private List<Integer> flush() {
			return Util.add(Arrays.asList(root), allocator.flush());
		}

		private Integer createRootPage(List<Slot> slots) {
			Slot slot;
			Integer pointer;
			if (slots.size() == 1 && (slot = slots.get(0)).type == SlotType.BRANCH)
				pointer = slot.pointer;
			else
				pointer = persist(slots);
			return pointer;
		}

		private Slot slot(List<Slot> slots) {
			return new Slot(SlotType.BRANCH, Util.first(slots).pivot, persist(slots));
		}

		private Slot discard(Slot slot) {
			if (slot != null && slot.type != SlotType.TERMINAL)
				allocator.discard(slot.pointer);
			return slot;
		}

		private Integer persist(List<Slot> slots) {
			Integer pointer = allocator.allocate();
			write(pointer, new Page(slots));
			return pointer;
		}
	}

	private class Mutate implements Closeable {
		private SerializedPageFile<List<Integer>> stampFile;

		private Mutate() {
			PageFileImpl stampPageFile = new PageFileImpl(filename + ".stamp", pageSize);
			stampFile = new SerializedPageFileImpl<>(stampPageFile, Serialize.list(Serialize.int_));
		}

		private Mutator begin() {
			return mutator(stampFile.load(0));
		}

		private void commit(Mutator mutator) {
			List<Integer> stamp = mutator.flush();
			sync();
			stampFile.save(0, stamp);
		}

		public void close() throws IOException {
			stampFile.close();
		}
	}

	/**
	 * Constructor for larger trees that require another tree for page
	 * allocation management.
	 */
	public IbTreeImpl(String filename, IbTreeConfiguration<Key> config, IbTreeImpl<Integer> allocationIbTree) {
		this.filename = filename;
		pageSize = config.getPageSize();
		comparator = Util.nullsFirst(config.getComparator());
		serializer = Serialize.nullable(config.getSerializer());
		maxBranchFactor = config.getMaxBranchFactor();
		this.allocationIbTree = allocationIbTree;

		int pageSize = config.getPageSize();

		mutate = new Mutate();
		minBranchFactor = maxBranchFactor / 2;
		pageFile0 = new PageFileImpl(filename, pageSize);
		pageFile = new SerializedPageFileImpl<>(pageFile0, createPageSerializer());
		payloadFile = new SerializedPageFileImpl<>(pageFile0, Serialize.bytes(pageSize));
	}

	@Override
	public void close() throws IOException {
		mutate.close();
		pageFile.close();
	}

	@Override
	public Mutator begin() {
		return mutate.begin();
	}

	@Override
	public int guaranteedCapacity() {
		if (allocationIbTree != null)
			// Refers the long pile above
			return allocationIbTree.guaranteedCapacity() * 9 / 10 * (minBranchFactor - 1) + 1;
		else
			// There are at most maxBranchFactor - 1 nodes, and need to keep 1
			// for the guard node too
			return maxBranchFactor - 2;
	}

	@Override
	public Mutator create() {
		List<Integer> stamp0;

		if (allocationIbTree != null) {
			IbTreeImpl<Integer>.Mutator mutator0 = allocationIbTree.create();
			int nPages = allocationIbTree.guaranteedCapacity();
			for (int p = 0; p < nPages; p++)
				mutator0.putTerminal(p);
			stamp0 = mutator0.flush();
		} else
			stamp0 = Arrays.asList(0);

		return new Mutator(allocator(stamp0));
	}

	private Streamlet<Key> keys(Integer pointer, Key start, Key end) {
		return stream(pointer, start, end).map(slot -> slot.pivot);
	}

	private Integer get0(Integer root, Key key, SlotType slotType) {
		Slot slot = stream(root, key, null).first();
		if (slot != null && slot.type == slotType && comparator.compare(slot.pivot, key) == 0)
			return slot.pointer;
		else
			return null;
	}

	private Streamlet<Slot> stream(Integer pointer, Key start, Key end) {
		List<Slot> node = read(pointer).slots;
		int i0 = start != null ? new FindSlot(node, start, false).i + 1 : 0;
		int i1 = end != null ? new FindSlot(node, end, false).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(Math.max(0, i0), i1)).concatMap(slot -> {
				if (slot.type == SlotType.BRANCH)
					return stream(slot.pointer, start, end);
				else
					return slot.pivot != null ? Read.from(slot) : Read.empty();
			});
		else
			return Read.empty();
	}

	private Mutator mutator(List<Integer> stamp) {
		return new Mutator(allocator(Util.right(stamp, 1)), stamp.get(0));
	}

	private Allocator allocator(List<Integer> stamp0) {
		Allocator allocator;
		if (allocationIbTree != null)
			allocator = new SubIbTreeAllocator(allocationIbTree.mutator(stamp0));
		else
			allocator = new SwappingAllocator(stamp0.get(0));
		return new DelayedDiscardAllocator(allocator);
	}

	private Page read(Integer pointer) {
		return pageFile.load(pointer);
	}

	private void write(Integer pointer, Page page) {
		pageFile.save(pointer, page);
	}

	private void sync() {
		pageFile0.sync();
	}

	private Serializer<Page> createPageSerializer() {
		Serializer<List<Slot>> slotsSerializer = Serialize.list(new Serializer<Slot>() {
			public Slot read(DataInput dataInput) throws IOException {
				SlotType type = SlotType.values()[dataInput.readByte()];
				Key pivot = serializer.read(dataInput);
				Integer pointer = pointerSerializer.read(dataInput);
				return new Slot(type, pivot, pointer);
			}

			public void write(DataOutput dataOutput, Slot slot) throws IOException {
				dataOutput.writeByte(slot.type.ordinal());
				serializer.write(dataOutput, slot.pivot);
				pointerSerializer.write(dataOutput, slot.pointer);
			}
		});

		return new Serializer<Page>() {
			public Page read(DataInput dataInput) throws IOException {
				return new Page(slotsSerializer.read(dataInput));
			}

			public void write(DataOutput dataOutput, Page page) throws IOException {
				slotsSerializer.write(dataOutput, page.slots);
			}
		};
	}

}
