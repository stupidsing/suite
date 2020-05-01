package suite.btree.impl;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.adt.Pair;
import primal.primitive.adt.Bytes;
import primal.streamlet.Streamlet;
import suite.btree.B_Tree;
import suite.file.PageAllocator;
import suite.file.SerializedPageFile;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import static primal.statics.Fail.fail;

/**
 * B+ tree implementation.
 *
 * @author ywsing
 */
public class B_TreeImpl<Key, Value> implements B_Tree<Key, Value> {

	private int branchFactor;
	private PageAllocator allocator;
	private SerializedPageFile<Superblock> superblockFile;
	private SerializedPageFile<Bytes> payloadFile;
	private SerializedPageFile<Page> pageFile;
	private Comparator<Key> comparator;

	public class Superblock {
		public int root;
	}

	public class Page extends ArrayList<KeyPointer> {
		private static final long serialVersionUID = 1l;
		public int pointer;

		public Page(int pointer) {
			this.pointer = pointer;
		}

		public Page(int pointer, List<KeyPointer> keyPointers) {
			super(keyPointers);
			this.pointer = pointer;
		}

		public KeyPointer keyPointer(int index) {
			return 0 <= index && index < size() ? get(index) : null;
		}
	}

	public class KeyPointer {
		public Key key;
		public Pointer pointer;

		public KeyPointer(Key key, Pointer pointer) {
			this.key = key;
			this.pointer = pointer;
		}

		public int branchPointer() {
			@SuppressWarnings("unchecked")
			var branch = (Branch) pointer;
			return branch.pointer;
		}

		public Value leafValue() {
			@SuppressWarnings("unchecked")
			var leaf = (Leaf) pointer;
			return leaf.value;
		}

		public int payloadPointer() {
			@SuppressWarnings("unchecked")
			var payload = (Payload) pointer;
			return payload.pointer;
		}
	}

	public class Branch implements Pointer {
		private int pointer;

		public Branch(int pointer) {
			this.pointer = pointer;
		}
	}

	public class Leaf implements Pointer {
		private Value value;

		public Leaf(Value value) {
			this.value = value;
		}
	}

	public class Payload implements Pointer {
		private int pointer;

		public Payload(int pointer) {
			this.pointer = pointer;
		}
	}

	public class Terminal implements Pointer {
	}

	public interface Pointer {
	}

	private class Slots extends Stack<Slot> {
		private static final long serialVersionUID = 1l;
	}

	private class Slot {
		private Page page;
		private Integer index;

		private Slot(Page page, Integer index) {
			this.page = page;
			this.index = index;
		}

		private KeyPointer getKeyPointer() {
			return page.keyPointer(index);
		}
	}

	private class Traverse {
		private Slots traverse = new Slots();
		private Page page;
		private int index;
		private KeyPointer kp;

		private Traverse(Key key) {
			Integer pointer = root();

			while (pointer != null) {
				page = pageFile.load(pointer);
				index = findPosition(page, key, true);
				kp = page.keyPointer(index);
				traverse.push(new Slot(page, index));

				pointer = kp != null && kp.pointer instanceof B_TreeImpl.Branch ? kp.branchPointer() : null;
			}
		}
	}

	public B_TreeImpl(Comparator<Key> comparator) {
		setBranchFactor(16);
		this.comparator = comparator;
	}

	@Override
	public void close() throws IOException {
		pageFile.close();
		superblockFile.close();
		allocator.close();
	}

	@Override
	public void create() {
		allocator.create();
		newRoot(List.of(new KeyPointer(null, new Terminal())));
	}

	@Override
	public Value get(Key key) {
		var kp = loadKeyPointer(key);
		return kp != null ? kp.leafValue() : null;
	}

	@Override
	public Bytes getPayload(Key key) {
		var kp = loadKeyPointer(key);
		return kp != null ? payloadFile.load(kp.payloadPointer()) : null;
	}

	@Override
	public boolean getTerminal(Key key) {
		return loadKeyPointer(key) != null;
	}

	private KeyPointer loadKeyPointer(Key key) {
		var kp = new Traverse(key).kp;
		return kp != null && Equals.ab(kp.key, key) ? kp : null;
	}

	@Override
	public Streamlet<Key> keys(Key key0, Key key1) {
		return stream(key0, key1).map(kp -> kp != null ? kp.key : null);
	}

	@Override
	public Streamlet<Pair<Key, Value>> range(Key key0, Key key1) {
		return stream(key0, key1).map(kp -> kp != null ? Pair.of(kp.key, kp.leafValue()) : null);
	}

	private Streamlet<KeyPointer> stream(Key start, Key end) {
		return stream_(root(), start, end).drop(1);
	}

	private Streamlet<KeyPointer> stream_(Integer pointer, Key start, Key end) {
		var page = pageFile.load(pointer);
		var i0 = start != null ? findPosition(page, start, false) : 0;
		var i1 = end != null ? findPosition(page, end, false) + 1 : page.size();

		if (i0 < i1)
			return Read.from(page.subList(i0, i1)).concatMap(kp -> {
				if (kp.pointer instanceof B_TreeImpl.Branch)
					return stream_(kp.branchPointer(), start, end);
				else
					return Read.each(kp);
			});
		else
			return Read.empty();
	}

	@Override
	public void put(Key key, Value value) {
		put(key, new Leaf(value));
	}

	@Override
	public void putPayload(Key key, Bytes bytes) {
		var pointer = allocator.allocate();
		payloadFile.save(pointer, bytes);
		put(key, new Payload(pointer));
	}

	@Override
	public void putTerminal(Key key) {
		put(key, new Terminal());
	}

	private void put(Key key, Pointer pointer) {
		var t = new Traverse(key);
		var kp = t.kp;

		if (kp != null && Equals.ab(kp.key, key)) {
			discard(kp);
			kp.pointer = pointer; // replace existing value
			saveOldPage(t.page);
		} else
			addAndSplit(t.traverse, new KeyPointer(key, pointer));
	}

	private void addAndSplit(Slots slots, KeyPointer toInsert) {
		var half = branchFactor / 2;
		boolean done;

		// traversed to deepest. Inserts key-value pair
		do {
			var slot = slots.pop();
			var page = slot.page;
			page.add(slot.index + 1, toInsert);

			var size = page.size();
			done = size <= branchFactor;

			if (!done) { // splits list into two pages
				int pointer0 = page.pointer, pointer1 = allocator.allocate();
				var p0 = new Page(pointer0, page.subList(0, half));
				var p1 = new Page(pointer1, page.subList(half, size));
				saveOldPage(p0);
				saveNewPage(p1);

				toInsert = pointerTo(p1); // propagates to parent

				if (slots.empty()) { // have to create a new root
					newRoot(List.of(pointerTo(p0), toInsert));
					done = true;
				}
			} else
				saveOldPage(page);
		} while (!done);
	}

	private void newRoot(List<KeyPointer> kps) {
		var root = allocator.allocate();

		var superblock = new Superblock();
		superblock.root = root;
		superblockFile.save(0, superblock);

		saveNewPage(new Page(root, kps));
	}

	@Override
	public void removePayload(Key key) {
		remove(key);
	}

	@Override
	public void removeTerminal(Key key) {
		remove(key);
	}

	@Override
	public void remove(Key key) {
		var half = branchFactor / 2;
		var root = root();
		var t = new Traverse(key);
		var slots = t.traverse;

		// remove the entry
		var slot = slots.pop();
		var page = slot.page;
		var index = slot.index;
		var kp = slot.getKeyPointer();

		if (kp != null && Equals.ab(kp.key, key)) {
			discard(kp);
			page.remove(index.intValue());
		}

		// rotates nodes around to maintain invariant
		while (page.pointer != root && page.size() < half) {
			var mp = page;

			slot = slots.pop();
			page = slot.page;
			index = slot.index;

			var lp = loadBranch(page, index - 1);
			var rp = loadBranch(page, index + 1);
			var lsize = lp != null ? lp.size() : 0;
			var rsize = rp != null ? rp.size() : 0;

			if (rsize <= lsize && lsize != 0)
				if (half < lsize) { // shift
					var out = lp.remove(lsize - 1);
					mp.add(0, out);
					saveOldPage(mp);
					saveOldPage(lp);
					page.set(index, pointerTo(mp));
				} else
					merge(page, lp, mp, index - 1);
			else if (lsize <= rsize && rsize != 0)
				if (half < rsize) { // shift
					var out = rp.remove(0);
					mp.add(out);
					saveOldPage(mp);
					saveOldPage(rp);
					page.set(index + 1, pointerTo(rp));
				} else
					merge(page, mp, rp, index);
			else if (slots.size() == 0) {

				// left/right node empty, should only happen at root node
				page.clear();
				page.addAll(mp);
				saveOldPage(page);
				allocator.deallocate(mp.pointer);
			} else
				fail("unbalanced B-tree");
		}

		saveOldPage(page);
	}

	/**
	 * Merge two consecutive branches in a page.
	 *
	 * p0 and p1 are branches of parent. p0 is located in slot 'index' of parent,
	 * while p1 is in next.
	 */
	private void merge(Page parent, Page p0, Page p1, int index) {
		p0.addAll(p1);
		saveOldPage(p0);
		allocator.deallocate(p1.pointer);
		parent.remove(index + 1);
	}

	private void dump(PrintStream w, String pfx, int pointer) {
		var page = loadPage(pointer);

		for (var kp : page) {
			var ptr = kp.pointer;
			w.print(pfx + (kp.key != null ? kp.key : "MIN-KEY"));

			if (ptr instanceof B_TreeImpl.Branch) {
				w.println();
				@SuppressWarnings("unchecked")
				var branch = (Branch) ptr;
				dump(w, pfx + "\t", branch.pointer);
			} else if (ptr instanceof B_TreeImpl.Leaf)
				w.println(" = " + kp.leafValue());
			else if (ptr instanceof B_TreeImpl.Payload)
				w.println(" <Payload>");
			else if (ptr instanceof B_TreeImpl.Terminal)
				w.println(" <Terminal>");
		}
	}

	private void discard(KeyPointer kp) {
		if (kp.pointer instanceof B_TreeImpl<?, ?>.Payload)
			allocator.deallocate(kp.payloadPointer());
	}

	private Page loadBranch(Page page, int index) {
		var kp = page.keyPointer(index);
		return kp != null && kp.pointer instanceof B_TreeImpl.Branch ? loadPage(kp.branchPointer()) : null;
	}

	private Page loadPage(int pointer) {
		return pageFile.load(pointer);
	}

	private void saveNewPage(Page page) {
		savePage(page);
	}

	private void saveOldPage(Page page) {
		savePage(page);
	}

	private void savePage(Page page) {
		pageFile.save(page.pointer, page);
	}

	private KeyPointer pointerTo(Page page) {
		var smallest = page.get(0).key;
		return new KeyPointer(smallest, new Branch(page.pointer));
	}

	private int findPosition(Page page, Key key, boolean isInclusive) {
		int c, i = page.size();
		while (0 < i)
			if ((c = comparator.compare(page.get(--i).key, key)) <= 0)
				if (isInclusive || c < 0)
					break;
		return i;
	}

	@Override
	public void dump(PrintStream w) {
		w.println("==========");
		dump(w, "", root());
	}

	private int root() {
		return superblockFile.load(0).root;
	}

	public void setBranchFactor(int branchFactor) {
		this.branchFactor = branchFactor;
	}

	public void setAllocator(PageAllocator allocator) {
		this.allocator = allocator;
	}

	public void setSuperblockPageFile(SerializedPageFile<Superblock> superblockFile) {
		this.superblockFile = superblockFile;
	}

	public void setPayloadFile(SerializedPageFile<Bytes> payloadFile) {
		this.payloadFile = payloadFile;
	}

	public void setPageFile(SerializedPageFile<Page> pageFile) {
		this.pageFile = pageFile;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

}
