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

	public int branchFactor, leafFactor;
	public Integer root;
	public Allocator allocator;
	public Persister<Page> persister;
	public Comparator<Key> comparator;

	public interface Pointer {
	}

	public class Leaf implements Pointer {
		public Value value;

		public Leaf(Value value) {
			this.value = value;
		}
	}

	public class Branch implements Pointer {
		public int branch;

		public Branch(int branch) {
			this.branch = branch;
		}
	}

	public class KeyPointer extends Pair<Key, Pointer> {
		public KeyPointer(Key t1, Pointer t2) {
			super(t1, t2);
		}
	}

	public class Page {
		public int pageNo;
		public List<KeyPointer> keyPointers = new ArrayList<>();

		public Page(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public class Traverse extends Stack<Pair<Page, Integer>> {
		private static final long serialVersionUID = 1l;
	}

	public B_Tree(Comparator<Key> comparator) {
		setBranchFactor(16);
		setLeafFactor(16);
		this.comparator = comparator;
	}

	public void create() {
		savePage(new Page(root = allocator.allocate()));
	}

	public Value get(Key key) {
		Traverse traverse = traverse(key);
		Pair<Page, Integer> last = traverse.peek();
		Page page = last.t1;
		Integer index = last.t2;

		if (index < page.keyPointers.size()) {
			Pair<Key, Pointer> keyPointer = page.keyPointers.get(index);

			if (Util.equals(keyPointer.t1, key)) {
				@SuppressWarnings("unchecked")
				Leaf leaf = (Leaf) keyPointer.t2;
				return leaf.value;
			}
		}

		return null;
	}

	public Iterable<Pair<Key, Value>> range(Key start, Key end) {
		final Traverse s = traverse(start);
		final Traverse e = traverse(end);

		return new Iterable<Pair<Key, Value>>() {
			public Iterator<Pair<Key, Value>> iterator() {
				return new Iterator<Pair<Key, Value>>() {
					private Traverse current = s;

					public boolean hasNext() {
						Pair<Page, Integer> k0 = current.peek();
						Pair<Page, Integer> k1 = e.peek();
						return k0.t1.pageNo != k1.t1.pageNo
								|| !Util.equals(k0.t2, k1.t2);
					}

					public Pair<Key, Value> next() {
						while (true) {
							Pair<Page, Integer> k = current.peek();
							k.t2++;

							if (k.t2 < k.t1.keyPointers.size()) {
								KeyPointer p = k.t1.keyPointers.get(k.t2);

								if (p.t2 instanceof B_Tree.Branch) {
									Page page = loadPage(k.t1, k.t2);
									current.push(Pair.create(page, 0));
								} else {
									@SuppressWarnings("unchecked")
									Leaf leaf = (Leaf) p.t2;
									return Pair.create(p.t1, leaf.value);
								}
							} else
								current.pop();
						}
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public void put(Key key, Value value) {
		Traverse trace = traverse(key);
		Pair<Page, Integer> pair = trace.peek();
		Page page = pair.t1;
		Integer index = pair.t2;
		KeyPointer keyPointer = null;
		boolean needInsert = true;

		if (index < page.keyPointers.size()) {
			keyPointer = page.keyPointers.get(index);

			if (Util.equals(keyPointer.t1, key)) { // Replace existing value?
				keyPointer.t2 = new Leaf(value);
				needInsert = false;
			}
		}

		if (needInsert)
			addAndSplit(trace, page, new KeyPointer(key, new Leaf(value)));
		else
			savePage(page);
	}

	private void addAndSplit(Traverse traverse, Page page, KeyPointer toInsert) {
		Pair<Page, Integer> pair;
		Integer index;

		// Traversed to deepest. Inserts key-value pair
		while (true) {
			pair = traverse.pop();
			page = pair.t1;
			index = pair.t2;
			page.keyPointers.add(index, toInsert);

			List<KeyPointer> keyPointers = page.keyPointers;
			int size = keyPointers.size();
			int maxNodes = getMaxNodes(page), half = maxNodes / 2;
			if (size <= maxNodes) {
				savePage(page);
				break;
			}

			// Splits list into the two pages
			Page p0 = new Page(allocator.allocate()), p1 = page;
			p0.keyPointers = new ArrayList<>(keyPointers.subList(0, half));
			p1.keyPointers = new ArrayList<>(keyPointers.subList(half, size));
			savePage(p0);
			savePage(p1);

			// Propagates to parent
			toInsert = pointerTo(p0);

			if (traverse.empty()) { // Have to create a new root
				page = new Page(root = allocator.allocate());
				addPointer(page, toInsert);
				addPointer(page, pointerTo(p1));
				savePage(page);
				break;
			}
		}
	}

	public void remove(Key key) {
		Stack<Pair<Page, Integer>> trace = traverse(key);
		Pair<Page, Integer> pair = trace.pop();
		Page page = pair.t1, childPage = null;
		int index = pair.t2;

		if (index >= page.keyPointers.size()
				|| !Util.equals(page.keyPointers.get(index).t1, key))
			return;

		page.keyPointers.remove(index);

		while (page.pageNo != root) {
			int half = getMaxNodes(page) / 2;
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

	private int getMaxNodes(Page page) {
		List<KeyPointer> ptrs = page.keyPointers;
		boolean isBranch = !ptrs.isEmpty()
				&& ptrs.get(0).t2 instanceof B_Tree.Branch;
		return isBranch ? branchFactor : leafFactor;
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
		Integer pageNo = root;

		while (pageNo != null) {
			page = persister.load(pageNo);
			int index = findPosition(page, key);
			traversed.push(Pair.create(page, index));

			pageNo = null;

			if (index < page.keyPointers.size()) {
				Pointer pointer = page.keyPointers.get(index).t2;
				if (pointer instanceof B_Tree.Branch) {
					@SuppressWarnings("unchecked")
					B_Tree<Key, Value>.Branch branch = (Branch) pointer;
					pageNo = branch.branch;
				}
			}
		}

		return traversed;
	}

	private void addPointer(Page page, KeyPointer keyPointer) {
		page.keyPointers.add(findPosition(page, keyPointer.t1), keyPointer);
	}

	private int findPosition(Page page, Key key) {
		int i, size = page.keyPointers.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(page.keyPointers.get(i).t1, key) >= 0)
				break;
		return i;
	}

	private KeyPointer pointerTo(Page page) {
		List<KeyPointer> keyPointers = page.keyPointers;
		Key largest = keyPointers.get(keyPointers.size() - 1).t1;
		return new KeyPointer(largest, new Branch(page.pageNo));
	}

	private Page loadPage(Page parent, int index) {
		if (index >= 0 && index < parent.keyPointers.size()) {
			Pointer pointer = parent.keyPointers.get(index).t2;
			if (pointer instanceof B_Tree.Branch) {
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) pointer;
				return persister.load(branch.branch);
			}
		}
		return null;
	}

	private void savePage(Page page) {
		persister.save(page.pageNo, page);
	}

	public void dump(PrintStream w) {
		w.println("==========");
		dump(w, "", root);
	}

	public void dump(PrintStream w, String pfx, int pageNo) {
		Page page = persister.load(pageNo);

		for (KeyPointer keyPointer : page.keyPointers) {
			Pointer ptr = keyPointer.t2;

			if (ptr instanceof B_Tree.Branch) {
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) ptr;
				dump(w, pfx + "\t", branch.branch);
				w.println(pfx + keyPointer.t1);
			} else {
				@SuppressWarnings("unchecked")
				Leaf leaf = (Leaf) ptr;
				w.println(pfx + keyPointer.t1 + " = " + leaf.value);
			}
		}
	}

	public void setBranchFactor(int branchFactor) {
		this.branchFactor = branchFactor;
	}

	public void setLeafFactor(int leafFactor) {
		this.leafFactor = leafFactor;
	}

	public void setRoot(Integer root) {
		this.root = root;
	}

	public void setAllocator(Allocator allocator) {
		this.allocator = allocator;
	}

	public void setPersister(Persister<Page> persister) {
		this.persister = persister;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

}
