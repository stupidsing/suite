package org.btree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
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
	public Persister<Page<Key>> persister;
	public Comparator<Key> comparator;

	public interface Pointer {
	}

	public static class Leaf<V> implements Pointer {
		public V value;

		public Leaf(V value) {
			this.value = value;
		}
	}

	public static class Branch implements Pointer {
		public int branch;

		public Branch(int branch) {
			this.branch = branch;
		}
	}

	public static class KeyPointer<K> extends Pair<K, Pointer> {
		public KeyPointer(K t1, Pointer t2) {
			super(t1, t2);
		}
	}

	public static class Page<K> {
		public int pageNo;
		public List<KeyPointer<K>> keyPointers = new ArrayList<>();

		public Page(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public B_Tree(Allocator allocator //
			, Persister<Page<Key>> persister //
			, Comparator<Key> comparator) {
		this(allocator, persister, comparator, allocator.allocate());
		savePage(new Page<Key>(root));
	}

	public B_Tree(Allocator allocator //
			, Persister<Page<Key>> persister //
			, Comparator<Key> comparator //
			, Integer root) {
		setBranchFactor(16);
		setLeafFactor(16);
		this.allocator = allocator;
		this.persister = persister;
		this.comparator = comparator;
		this.root = root;
	}

	public Value get(Key key) {
		Stack<Pair<Page<Key>, Integer>> traverse = traverse(key);
		Pair<Page<Key>, Integer> last = traverse.peek();
		Page<Key> page = last.t1;
		Integer index = last.t2;

		if (index < page.keyPointers.size()) {
			Pair<Key, Pointer> keyPointer = page.keyPointers.get(index);

			if (Util.equals(keyPointer.t1, key)) {
				@SuppressWarnings("unchecked")
				Leaf<Value> leaf = (Leaf<Value>) keyPointer.t2;
				return leaf.value;
			}
		}

		return null;
	}

	public void put(Key key, Value value) {
		Stack<Pair<Page<Key>, Integer>> trace = traverse(key);
		Pair<Page<Key>, Integer> pair = trace.peek();
		Page<Key> page = pair.t1;
		Integer index = pair.t2;
		KeyPointer<Key> keyPointer = null;
		boolean needInsert = true;

		if (index < page.keyPointers.size()) {
			keyPointer = page.keyPointers.get(index);

			if (Util.equals(keyPointer.t1, key)) { // Replace existing value?
				keyPointer.t2 = new Leaf<>(value);
				needInsert = false;
			}
		}

		if (needInsert)
			addAndSplit(trace, page, new KeyPointer<>(key, new Leaf<>(value)));
		else
			savePage(page);
	}

	private void addAndSplit(Stack<Pair<Page<Key>, Integer>> trace,
			Page<Key> page, KeyPointer<Key> toInsert) {
		Pair<Page<Key>, Integer> pair;
		Integer index;

		// Traversed to deepest. Inserts key-value pair
		while (true) {
			pair = trace.pop();
			page = pair.t1;
			index = pair.t2;
			page.keyPointers.add(index, toInsert);

			List<KeyPointer<Key>> keyPointers = page.keyPointers;
			int size = keyPointers.size();
			int maxNodes = getMaxNodes(page), half = maxNodes / 2;
			if (size <= maxNodes) {
				savePage(page);
				break;
			}

			// Splits list into the two pages
			Page<Key> p0 = new Page<>(allocator.allocate()), p1 = page;
			p0.keyPointers = new ArrayList<>(keyPointers.subList(0, half));
			p1.keyPointers = new ArrayList<>(keyPointers.subList(half, size));
			savePage(p0);
			savePage(p1);

			// Propagates to parent
			toInsert = pointerTo(p0);

			if (trace.empty()) { // Have to create a new root
				page = new Page<>(root = allocator.allocate());
				addPointer(page, toInsert);
				addPointer(page, pointerTo(p1));
				savePage(page);
				break;
			}
		}
	}

	public void remove(Key key) {
		Stack<Pair<Page<Key>, Integer>> trace = traverse(key);
		Pair<Page<Key>, Integer> pair = trace.pop();
		Page<Key> page = pair.t1, childPage = null;
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

			Page<Key> lp = loadPage(page, index - 1);
			Page<Key> rp = loadPage(page, index + 1);
			int lsize = lp != null ? lp.keyPointers.size() : 0;
			int rsize = rp != null ? rp.keyPointers.size() : 0;

			if (lsize >= rsize && lsize != 0)
				if (lsize > half) { // Shift
					KeyPointer<Key> out = lp.keyPointers.remove(lsize - 1);
					childPage.keyPointers.add(0, out);
					savePage(childPage);
					savePage(lp);
					page.keyPointers.set(index - 1, pointerTo(lp));
				} else
					merge(page, lp, childPage, index - 1);
			else if (rsize >= lsize && rsize != 0)
				if (rsize > half) { // Shift
					KeyPointer<Key> out = rp.keyPointers.remove(0);
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

	private int getMaxNodes(Page<Key> page) {
		List<KeyPointer<Key>> ptrs = page.keyPointers;
		boolean isBranch = !ptrs.isEmpty() && ptrs.get(0).t2 instanceof Branch;
		return isBranch ? branchFactor : leafFactor;
	}

	/**
	 * Merge two successive branches of a page.
	 * 
	 * p0 and p1 are branches of parent. p0 is located in slot 'index' of
	 * parent, while p1 is in next.
	 */
	private void merge(Page<Key> parent, Page<Key> p0, Page<Key> p1, int index) {
		p1.keyPointers.addAll(0, p0.keyPointers);
		savePage(p1);
		allocator.deallocate(p0.pageNo);
		parent.keyPointers.remove(index);
	}

	private Stack<Pair<Page<Key>, Integer>> traverse(Key key) {
		Stack<Pair<Page<Key>, Integer>> walked = new Stack<>();
		Page<Key> page = null;
		Integer pageNo = root;

		while (pageNo != null) {
			page = persister.load(pageNo);
			int index = findPosition(page, key);
			walked.push(Pair.create(page, index));

			pageNo = null;

			if (index < page.keyPointers.size()) {
				Pointer pointer = page.keyPointers.get(index).t2;
				if (pointer instanceof Branch)
					pageNo = ((Branch) pointer).branch;
			}
		}

		return walked;
	}

	private void addPointer(Page<Key> page, KeyPointer<Key> keyPointer) {
		page.keyPointers.add(findPosition(page, keyPointer.t1), keyPointer);
	}

	private int findPosition(Page<Key> page, Key key) {
		int i, size = page.keyPointers.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(page.keyPointers.get(i).t1, key) >= 0)
				break;
		return i;
	}

	private KeyPointer<Key> pointerTo(Page<Key> page) {
		List<KeyPointer<Key>> keyPointers = page.keyPointers;
		Key largest = keyPointers.get(keyPointers.size() - 1).t1;
		return new KeyPointer<>(largest, new Branch(page.pageNo));
	}

	private Page<Key> loadPage(Page<Key> parent, int index) {
		if (index >= 0 && index < parent.keyPointers.size()) {
			Pointer pointer = parent.keyPointers.get(index).t2;
			if (pointer instanceof Branch)
				return persister.load(((Branch) pointer).branch);
		}
		return null;
	}

	private void savePage(Page<Key> page) {
		persister.save(page.pageNo, page);
	}

	public void dump(PrintStream w) {
		w.println("==========");
		dump(w, "", root);
	}

	public void dump(PrintStream w, String pfx, int pageNo) {
		Page<Key> page = persister.load(pageNo);

		for (KeyPointer<Key> keyPointer : page.keyPointers) {
			Pointer ptr = keyPointer.t2;

			if (ptr instanceof Branch) {
				dump(w, pfx + "\t", ((Branch) ptr).branch);
				w.println(pfx + keyPointer.t1);
			} else
				w.println(pfx + keyPointer.t1 + " = " + ((Leaf<?>) ptr).value);
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

	public void setPersister(Persister<Page<Key>> persister) {
		this.persister = persister;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

}
