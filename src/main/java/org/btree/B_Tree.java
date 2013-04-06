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

	public class Traverse extends Stack<Pair<Page, Integer>> {
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
		Traverse traverse = traverse(key);
		Pair<Page, Integer> last = traverse.peek();
		KeyPointer keyPointer = getKeyPointer(last);

		if (keyPointer != null && Util.equals(keyPointer.key, key))
			return getLeafValue(keyPointer);
		else
			return null;
	}

	public Iterable<Pair<Key, Value>> range(Key key0, Key key1) {
		final Traverse start = traverse(key0);
		final Traverse end = traverse(key1);

		final Iterator<Pair<Key, Value>> iterator = new Iterator<Pair<Key, Value>>() {
			private Traverse traverse = start;
			private Pair<Key, Value> current;

			{
				KeyPointer kp = getKeyPointer(start.peek());

				if (kp.pointer instanceof B_Tree.Branch)
					next(); // No result for start, search next
				else
					current = Pair.create(kp.key, getLeafValue(kp));
			}

			public boolean hasNext() {
				Pair<Page, Integer> p0 = traverse.peek();
				Pair<Page, Integer> p1 = end.peek();
				return p0.t1.pageNo != p1.t1.pageNo
						|| !Util.equals(p0.t2, p1.t2);
			}

			public Pair<Key, Value> next() {
				Pair<Key, Value> current0 = current;

				while (true) {
					Pair<Page, Integer> k = traverse.peek();
					k.t2++;
					KeyPointer kp = getKeyPointer(k);

					if (kp != null)
						if (kp.pointer instanceof B_Tree.Branch) {
							Page page = loadPage(k.t1, k.t2);
							traverse.push(Pair.create(page, 0));
						} else {
							current = Pair.create(kp.key, getLeafValue(kp));
							return current0;
						}
					else
						traverse.pop();
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};

		return new Iterable<Pair<Key, Value>>() {
			public Iterator<Pair<Key, Value>> iterator() {
				return iterator;
			}
		};
	}

	public void put(Key key, Value value) {
		Traverse trace = traverse(key);
		Pair<Page, Integer> pair = trace.peek();
		Page page = pair.t1;
		KeyPointer keyPointer = getKeyPointer(pair);

		if (keyPointer != null && Util.equals(keyPointer.key, key)) {
			keyPointer.pointer = new Leaf(value); // Replace existing value
			savePage(page);
		} else
			addAndSplit(trace, page, new KeyPointer(key, new Leaf(value)));
	}

	private void addAndSplit(Traverse traverse, Page page, KeyPointer toInsert) {
		Pair<Page, Integer> pair;
		Integer index;
		boolean done = false;

		// Traversed to deepest. Inserts key-value pair
		do {
			pair = traverse.pop();
			page = pair.t1;
			index = pair.t2;
			page.keyPointers.add(index, toInsert);

			List<KeyPointer> kps = page.keyPointers;
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

				if (traverse.empty()) { // Have to create a new root
					create();
					page = new Page(getRoot());
					page.keyPointers.add(toInsert);
					page.keyPointers.add(pointerTo(p1));
					savePage(page);
					break;
				}
			} else
				savePage(page);
		} while (!done);
	}

	public void remove(Key key) {
		Stack<Pair<Page, Integer>> trace = traverse(key);
		Pair<Page, Integer> pair = trace.pop();
		Page page = pair.t1, childPage = null;
		int index = pair.t2;
		KeyPointer keyPointer = getKeyPointer(pair);

		if (keyPointer == null || !Util.equals(keyPointer.key, key))
			return;

		int root = getRoot();
		page.keyPointers.remove(index);

		while (page.pageNo != root) {
			int half = branchFactor / 2;
			if (page.keyPointers.size() >= half)
				break;

			childPage = page;

			pair = trace.pop();
			page = pair.t1;
			index = pair.t2;

			Page lp = loadPage(page, index - 1);
			Page rp = loadPage(page, index + 1);
			int lsize = lp != null ? lp.keyPointers.size() : 0;
			int rsize = rp != null ? rp.keyPointers.size() : 0;

			if (lsize >= rsize && lsize != 0)
				if (lsize > half) { // Shift
					KeyPointer out = lp.keyPointers.remove(lsize - 1);
					childPage.keyPointers.add(0, out);
					savePage(childPage);
					savePage(lp);
					page.keyPointers.set(index - 1, pointerTo(lp));
				} else
					merge(page, lp, childPage, index - 1);
			else if (rsize >= lsize && rsize != 0)
				if (rsize > half) { // Shift
					KeyPointer out = rp.keyPointers.remove(0);
					childPage.keyPointers.add(out);
					savePage(childPage);
					savePage(rp);
					page.keyPointers.set(index, pointerTo(childPage));
				} else
					merge(page, childPage, rp, index);
			else {
				// Left/right node empty, should not happen if re-balanced well
				page.keyPointers = childPage.keyPointers;
				savePage(page);
				allocator.deallocate(childPage.pageNo);
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

	private Traverse traverse(Key key) {
		Traverse traversed = new Traverse();
		Page page = null;
		Integer pageNo = getRoot();

		while (pageNo != null) {
			page = pagePersister.load(pageNo);
			int index = findPosition(page, key);
			traversed.push(Pair.create(page, index));

			pageNo = null;

			if (index < page.keyPointers.size()) {
				KeyPointer kp = page.keyPointers.get(index);

				if (kp.pointer instanceof B_Tree.Branch)
					pageNo = toBranch(kp);
			}
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

	int toBranch(KeyPointer keyPointer) {
		@SuppressWarnings("unchecked")
		Branch branch = (Branch) keyPointer.pointer;
		return branch.pageNo;
	}

	Value getLeafValue(KeyPointer keyPointer) {
		@SuppressWarnings("unchecked")
		Leaf leaf = (Leaf) keyPointer.pointer;
		return leaf.value;
	}

	private KeyPointer getKeyPointer(Pair<Page, Integer> pair) {
		List<KeyPointer> keyPointers = pair.t1.keyPointers;

		if (pair.t2 < keyPointers.size())
			return keyPointers.get(pair.t2);
		else
			return null;
	}

	private KeyPointer pointerTo(Page page) {
		List<KeyPointer> keyPointers = page.keyPointers;
		Key largest = keyPointers.get(keyPointers.size() - 1).key;
		return new KeyPointer(largest, new Branch(page.pageNo));
	}

	private Page loadPage(Page parent, int index) {
		if (index >= 0 && index < parent.keyPointers.size()) {
			Pointer pointer = parent.keyPointers.get(index).pointer;

			if (pointer instanceof B_Tree.Branch) {
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) pointer;
				return loadPage(branch.pageNo);
			}
		}

		return null;
	}

	private Page loadPage(int pageNo) {
		return pagePersister.load(pageNo);
	}

	private void savePage(Page page) {
		pagePersister.save(page.pageNo, page);
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
