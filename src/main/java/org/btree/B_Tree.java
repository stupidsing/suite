package org.btree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.util.Util;
import org.util.Util.Pair;

/**
 * B+ tree implementation.
 * 
 * @author ywsing
 */
public class B_Tree<Key, Value> {

	public int branchFactor;
	public Allocator allocator;
	public Persister<SuperBlock> superBlockPersister;
	public Persister<Page> pagePersister;
	public Comparator<Key> comparator;

	public interface Pointer {
	}

	public class Leaf implements Pointer {
		Value value;

		public Leaf(Value value) {
			this.value = value;
		}
	}

	public class Branch implements Pointer {
		int pageNo;

		public Branch(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public class KeyPointer {
		Key key;
		Pointer pointer;

		KeyPointer(Key key, Pointer pointer) {
			this.key = key;
			this.pointer = pointer;
		}
	}

	public class Page {
		int pageNo;
		List<KeyPointer> keyPointers = new ArrayList<>();

		public Page(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public class Slot {
		Page page;
		Integer index;

		public Slot(Page page, Integer index) {
			this.page = page;
			this.index = index;
		}
	}

	public class Slots extends Stack<Slot> {
		private static final long serialVersionUID = 1l;
	}

	public class SuperBlock {
		int root;
	}

	public B_Tree(Comparator<Key> comparator) {
		setBranchFactor(16);
		this.comparator = comparator;
	}

	public void create() {
		int root = allocator.allocate();
		setRoot(root);
		savePage(new Page(root));
	}

	public Value get(Key key) {
		Slot lastSlot = traverse(key).peek();
		KeyPointer keyPointer = getKeyPointer(lastSlot);

		if (keyPointer != null && Util.equals(keyPointer.key, key))
			return getLeafValue(keyPointer);
		else
			return null;
	}

	public Iterable<Pair<Key, Value>> range(Key startKey, Key endKey) {
		final Slots startSlots = traverse(startKey);
		final Slots endSlots = traverse(endKey);

		final Iterator<Pair<Key, Value>> iterator = new Iterator<Pair<Key, Value>>() {
			private Slots currentSlots = startSlots;
			private Pair<Key, Value> current;

			{
				KeyPointer kp = getKeyPointer(startSlots.peek());

				if (kp != null)
					if (kp.pointer instanceof B_Tree.Branch)
						next(); // No result for start, search next
					else
						current = Pair.create(kp.key, getLeafValue(kp));
			}

			public boolean hasNext() {
				Slot currentSlot = currentSlots.peek();
				Slot endSlot = endSlots.peek();
				return currentSlot.page.pageNo != endSlot.page.pageNo
						|| !Util.equals(currentSlot.index, endSlot.index);
			}

			public Pair<Key, Value> next() {
				Pair<Key, Value> current0 = current;
				current = advance();
				return current0;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			private Pair<Key, Value> advance() {
				currentSlots.peek().index++;

				while (true) {
					Slot slot = currentSlots.peek();
					KeyPointer kp = getKeyPointer(slot);

					if (kp != null)
						if (kp.pointer instanceof B_Tree.Branch) {
							Page page = loadPage(slot.page, slot.index);
							currentSlots.push(new Slot(page, 0));
						} else
							return Pair.create(kp.key, getLeafValue(kp));
					else if (currentSlots.size() != 1) {
						currentSlots.pop();
						advance();
					} else
						return null;
				}
			}
		};

		return new Iterable<Pair<Key, Value>>() {
			public Iterator<Pair<Key, Value>> iterator() {
				return iterator;
			}
		};
	}

	public void put(Key key, Value value) {
		Slots slots = traverse(key);
		Slot slot = slots.peek();
		KeyPointer keyPointer = getKeyPointer(slot);

		if (keyPointer != null && Util.equals(keyPointer.key, key)) {
			keyPointer.pointer = new Leaf(value); // Replace existing value
			savePage(slot.page);
		} else
			addAndSplit(slots, new KeyPointer(key, new Leaf(value)));
	}

	private void addAndSplit(Slots slots, KeyPointer toInsert) {
		boolean done = false;

		// Traversed to deepest. Inserts key-value pair
		do {
			Slot slot = slots.pop();
			Page page = slot.page;
			List<KeyPointer> kps = page.keyPointers;

			kps.add(slot.index, toInsert);

			int size = kps.size();
			done = size <= branchFactor;

			if (!done) { // Splits list into two pages
				int half = branchFactor / 2;
				Page p0 = new Page(allocator.allocate()), p1 = page;
				p0.keyPointers = new ArrayList<>(kps.subList(0, half));
				p1.keyPointers = new ArrayList<>(kps.subList(half, size));
				savePage(p0);
				savePage(p1);

				// Propagates to parent
				toInsert = pointerTo(p0);

				if (slots.empty()) { // Have to create a new root
					create();
					page = new Page(getRoot());
					page.keyPointers.add(toInsert);
					page.keyPointers.add(pointerTo(p1));
					savePage(page);
					done = true;
				}
			} else
				savePage(page);
		} while (!done);
	}

	public void remove(Key key) {
		Stack<Slot> traverse = traverse(key);
		Slot slot = traverse.pop();
		Page page = slot.page;
		int index = slot.index;
		KeyPointer keyPointer = getKeyPointer(slot);

		if (keyPointer == null || !Util.equals(keyPointer.key, key))
			return;

		int root = getRoot();
		page.keyPointers.remove(index);

		while (page.pageNo != root) {
			int half = branchFactor / 2;
			if (page.keyPointers.size() >= half)
				break;

			Page mp = page;

			slot = traverse.pop();
			page = slot.page;
			index = slot.index;

			Page lp = loadPage(page, index - 1);
			Page rp = loadPage(page, index + 1);
			int lsize = lp != null ? lp.keyPointers.size() : 0;
			int rsize = rp != null ? rp.keyPointers.size() : 0;

			if (lsize >= rsize && lsize != 0)
				if (lsize > half) { // Shift
					KeyPointer out = lp.keyPointers.remove(lsize - 1);
					mp.keyPointers.add(0, out);
					savePage(mp);
					savePage(lp);
					page.keyPointers.set(index - 1, pointerTo(lp));
				} else
					merge(page, lp, mp, index - 1);
			else if (rsize >= lsize && rsize != 0)
				if (rsize > half) { // Shift
					KeyPointer out = rp.keyPointers.remove(0);
					mp.keyPointers.add(out);
					savePage(mp);
					savePage(rp);
					page.keyPointers.set(index, pointerTo(mp));
				} else
					merge(page, mp, rp, index);
			else {
				// Left/right node empty, should not happen if re-balanced well
				page.keyPointers = mp.keyPointers;
				savePage(page);
				allocator.deallocate(mp.pageNo);
			}
		}

		savePage(page);
	}

	/**
	 * Merge two successive branches of a page.
	 * 
	 * p0 and p1 are branches of parent. p0 is located in slot 'index' of
	 * parent, while p1 is in next.
	 */
	private void merge(Page parent, Page p0, Page p1, int index) {
		p1.keyPointers.addAll(0, p0.keyPointers);
		savePage(p1);
		allocator.deallocate(p0.pageNo);
		parent.keyPointers.remove(index);
	}

	private Slots traverse(Key key) {
		Slots traversed = new Slots();
		Page page = null;
		Integer pageNo = getRoot();

		while (pageNo != null) {
			page = pagePersister.load(pageNo);
			int index = findPosition(page, key);
			KeyPointer kp = getKeyPointer(page, index);

			traversed.push(new Slot(page, index));

			if (kp != null && kp.pointer instanceof B_Tree.Branch)
				pageNo = getBranchPageNo(kp);
			else
				pageNo = null;
		}

		return traversed;
	}

	private int findPosition(Page page, Key key) {
		int i, size = page.keyPointers.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(page.keyPointers.get(i).key, key) >= 0)
				break;
		return i;
	}

	public void dump(PrintStream w, String pfx, int pageNo) {
		Page page = loadPage(pageNo);

		for (KeyPointer kp : page.keyPointers) {
			Pointer ptr = kp.pointer;

			if (ptr instanceof B_Tree.Branch) {
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) ptr;
				dump(w, pfx + "\t", branch.pageNo);
				w.println(pfx + kp.key);
			} else
				w.println(pfx + kp.key + " = " + getLeafValue(kp));
		}
	}

	private KeyPointer getKeyPointer(Slot slot) {
		return getKeyPointer(slot.page, slot.index);
	}

	private KeyPointer getKeyPointer(Page page, Integer index) {
		List<KeyPointer> keyPointers = page.keyPointers;

		if (index >= 0 && index < keyPointers.size())
			return keyPointers.get(index);
		else
			return null;
	}

	private Page loadPage(Page parent, int index) {
		if (index >= 0 && index < parent.keyPointers.size()) {
			KeyPointer keyPointer = parent.keyPointers.get(index);

			if (keyPointer.pointer instanceof B_Tree.Branch)
				return loadPage(getBranchPageNo(keyPointer));
		}

		return null;
	}

	private Page loadPage(int pageNo) {
		return pagePersister.load(pageNo);
	}

	private void savePage(Page page) {
		pagePersister.save(page.pageNo, page);
	}

	int getBranchPageNo(KeyPointer keyPointer) {
		@SuppressWarnings("unchecked")
		Branch branch = (Branch) keyPointer.pointer;
		return branch.pageNo;
	}

	Value getLeafValue(KeyPointer keyPointer) {
		@SuppressWarnings("unchecked")
		Leaf leaf = (Leaf) keyPointer.pointer;
		return leaf.value;
	}

	private KeyPointer pointerTo(Page page) {
		List<KeyPointer> keyPointers = page.keyPointers;
		Key largest = keyPointers.get(keyPointers.size() - 1).key;
		return new KeyPointer(largest, new Branch(page.pageNo));
	}

	public void dump(PrintStream w) {
		w.println("==========");
		dump(w, "", getRoot());
	}

	private int getRoot() {
		return superBlockPersister.load(0).root;
	}

	private void setRoot(int root) {
		SuperBlock superBlock = superBlockPersister.load(0);
		superBlock = superBlock != null ? superBlock : new SuperBlock();
		superBlock.root = root;
		superBlockPersister.save(0, superBlock);
	}

	public void setBranchFactor(int branchFactor) {
		this.branchFactor = branchFactor;
	}

	public void setAllocator(Allocator allocator) {
		this.allocator = allocator;
	}

	public void setSuperBlockPersister(Persister<SuperBlock> superBlockPersister) {
		this.superBlockPersister = superBlockPersister;
	}

	public void setPagePersister(Persister<Page> pagePersister) {
		this.pagePersister = pagePersister;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

}
