package org.btree;

import java.io.PrintStream;
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

	public int branchFactor;
	public Integer root;
	public Persister<Page> persister;
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
		int branch;

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
		public List<KeyPointer> keyPointers = Util.createList();

		public Page(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public B_Tree(Persister<Page> persister, Comparator<Key> comparator) {
		this(persister, comparator, persister.allocate());
		Page rootPage = new Page(root);
		save(rootPage);
	}

	public B_Tree(Persister<Page> persister, Comparator<Key> comparator,
			Integer root) {
		setBranchFactor(256);
		this.persister = persister;
		this.comparator = comparator;
		this.root = root;
	}

	public Value get(Key key) {
		Stack<Pair<Page, Integer>> traverse = traverse(key);
		Pair<Page, Integer> last = traverse.peek();
		Page page = last.t1;
		Integer index = last.t2;

		if (index < page.keyPointers.size()) {
			Pair<Key, Pointer> keyPointer = page.keyPointers.get(index);

			if (Util.equals(keyPointer.t1, key))
				return ((Leaf) keyPointer.t2).value;
		}

		return null;
	}

	public void put(Key key, Value value) {
		Stack<Pair<Page, Integer>> trace = traverse(key);
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
			page = addAndSplit(trace, page,
					new KeyPointer(key, new Leaf(value)));

		save(page);
	}

	private Page addAndSplit(Stack<Pair<Page, Integer>> trace, Page page,
			KeyPointer toInsert) {
		int half = branchFactor / 2;
		Pair<Page, Integer> pair;
		Integer index;

		// Traversed to deepest. Inserts key-value pair
		while (true) {
			pair = trace.pop();
			page = pair.t1;
			index = pair.t2;
			page.keyPointers.add(index, toInsert);

			List<KeyPointer> keyPointers = page.keyPointers;
			int size = keyPointers.size();
			if (size <= branchFactor)
				break;

			// Splits list into the two pages
			Page p1 = new Page(persister.allocate()), p2 = page;
			p1.keyPointers = Util.createList(keyPointers.subList(0, half));
			p2.keyPointers = Util.createList(keyPointers.subList(half, size));
			save(p1);
			save(p2);

			// Propagates to parent
			toInsert = new KeyPointer(largest(p1), new Branch(p1.pageNo));

			if (trace.empty()) { // Have to create a new root
				page = new Page(root = persister.allocate());
				add(page, toInsert);
				add(page, new KeyPointer(largest(p2), new Branch(p2.pageNo)));
				break;
			}
		}

		return page;
	}

	public void remove(Key key) {
		int half = branchFactor / 2;
		Stack<Pair<Page, Integer>> trace = traverse(key);
		Pair<Page, Integer> pair = trace.pop();
		Page page = pair.t1, childPage = null;
		int index = pair.t2;

		if (index >= page.keyPointers.size()
				|| !Util.equals(page.keyPointers.get(index).t1, key))
			return;

		page.keyPointers.remove(index);

		while (page.pageNo != root && page.keyPointers.size() < half) {
			childPage = page;

			pair = trace.pop();
			page = pair.t1;
			index = pair.t2;

			Page lp = loadPageIfExists(page, index - 1);
			Page rp = loadPageIfExists(page, index + 1);
			int lsize = (lp != null) ? lp.keyPointers.size() : 0;
			int rsize = (rp != null) ? rp.keyPointers.size() : 0;

			if (lsize >= rsize && lsize != 0)
				if (lsize <= half) // Merge
					merge(page, lp, childPage, index - 1);
				else { // Shift
					childPage.keyPointers.add(0, lp.keyPointers
							.remove(lp.keyPointers.size() - 1));
					save(childPage);
					save(lp);
					page.keyPointers.get(index - 1).t1 = largest(lp);
				}
			else if (rsize >= lsize && rsize != 0)
				if (rsize <= half) // Merge
					merge(page, childPage, rp, index);
				else { // Shift
					childPage.keyPointers.add(rp.keyPointers.remove(0));
					save(childPage);
					save(rp);
					page.keyPointers.get(index).t1 = largest(childPage);
				}
			else
				// Left/right node empty, should not happen if re-balanced well
				page.keyPointers = childPage.keyPointers;
		}

		if (page.pageNo == root && childPage != null
				&& page.keyPointers.size() == 1) {
			persister.deallocate(root);
			root = (page = childPage).pageNo;
		}

		save(page);
	}

	private Page loadPageIfExists(Page parent, int index) {
		if (index >= 0 && index < parent.keyPointers.size()) {
			Pointer pointer = parent.keyPointers.get(index).t2;
			if (pointer instanceof B_Tree<?, ?>.Branch)
				return persister.load(((Branch) pointer).branch);
		}
		return null;
	}

	/**
	 * Merges two successive branches of a page.
	 * 
	 * p1 and p2 are branches of parent. p1 is located in slot 'index' of
	 * parent, while p2 is in next.
	 */
	private void merge(Page parent, Page p1, Page p2, int index) {
		p2.keyPointers.addAll(0, p1.keyPointers);
		persister.save(p2.pageNo, p2);
		persister.deallocate(p1.pageNo);
		parent.keyPointers.remove(index);
	}

	private Stack<Pair<Page, Integer>> traverse(Key key) {
		Stack<Pair<Page, Integer>> walked = Util.createStack();
		Page page = null;
		Integer pageNo = root;

		while (pageNo != null) {
			page = persister.load(pageNo);
			int index = findPosition(page, key);
			walked.push(Pair.create(page, index));

			pageNo = null;
			if (index < page.keyPointers.size()) {
				Pointer pointer = page.keyPointers.get(index).t2;
				if (pointer instanceof B_Tree<?, ?>.Branch)
					pageNo = ((Branch) pointer).branch;
			}
		}

		return walked;
	}

	private void add(Page page, KeyPointer keyPointer) {
		page.keyPointers.add(findPosition(page, keyPointer.t1), keyPointer);
	}

	private int findPosition(Page page, Key key) {
		int i, size = page.keyPointers.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(page.keyPointers.get(i).t1, key) >= 0)
				break;
		return i;
	}

	private Key largest(Page page) {
		return page.keyPointers.get(page.keyPointers.size() - 1).t1;
	}

	private void save(Page page) {
		persister.save(page.pageNo, page);
	}

	public void dump(PrintStream w) {
		dump(w, "", root);
	}

	public void dump(PrintStream w, String pfx, int pageNo) {
		Page page = persister.load(pageNo);
		for (KeyPointer keyPointer : page.keyPointers) {
			Pointer ptr = keyPointer.t2;

			if (ptr instanceof B_Tree<?, ?>.Branch) {
				dump(w, pfx + "\t", ((Branch) ptr).branch);
				w.println(pfx + keyPointer.t1);
			} else
				w.println(pfx + keyPointer.t1 + " = " + ((Leaf) ptr).value);
		}
	}

	public void setBranchFactor(int branchFactor) {
		this.branchFactor = branchFactor;
	}

	public void setRoot(Integer root) {
		this.root = root;
	}

	public void setPersister(Persister<Page> persister) {
		this.persister = persister;
	}

	public void setCompare(Comparator<Key> compare) {
		this.comparator = compare;
	}

}
