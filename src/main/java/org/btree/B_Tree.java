package org.btree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
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

	private int branchFactor;
	private Allocator allocator;
	private Persister<SuperBlock> superBlockPersister;
	private Persister<Page> pagePersister;
	private Comparator<Key> comparator;

	public interface Pointer {
	}

	public class Leaf implements Pointer {
		public Value value;

		public Leaf(Value value) {
			this.value = value;
		}
	}

	public class Branch implements Pointer {
		public int pageNo;

		public Branch(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public class KeyPointer {
		public Key key;
		public Pointer pointer;

		KeyPointer(Key key, Pointer pointer) {
			this.key = key;
			this.pointer = pointer;
		}

		public int getBranchPageNo() {
			@SuppressWarnings("unchecked")
			Branch branch = (Branch) pointer;
			return branch.pageNo;
		}

		public Value getLeafValue() {
			@SuppressWarnings("unchecked")
			Leaf leaf = (Leaf) pointer;
			return leaf.value;
		}
	}

	public class Page extends ArrayList<KeyPointer> {
		private static final long serialVersionUID = 1l;
		public int pageNo;

		public Page(int pageNo) {
			super();
			this.pageNo = pageNo;
		}

		public Page(int pageNo, List<KeyPointer> keyPointers) {
			super(keyPointers);
			this.pageNo = pageNo;
		}
	}

	public class Slot {
		public Page page;
		public Integer index;

		public Slot(Page page, Integer index) {
			this.page = page;
			this.index = index;
		}

		private KeyPointer getKeyPointer() {
			return B_Tree.this.getKeyPointer(page, index);
		}
	}

	public class Slots extends Stack<Slot> {
		private static final long serialVersionUID = 1l;
	}

	public class SuperBlock {
		public int root;
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
		KeyPointer keyPointer = lastSlot.getKeyPointer();

		if (keyPointer != null && Util.equals(keyPointer.key, key))
			return keyPointer.getLeafValue();
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
				KeyPointer kp = startSlots.peek().getKeyPointer();

				if (kp != null)
					if (kp.pointer instanceof B_Tree.Branch)
						next(); // No result for start, search next
					else
						current = Pair.create(kp.key, kp.getLeafValue());
			}

			public boolean hasNext() {
				Slot currentSlot = currentSlots.peek();
				Slot endSlot = endSlots.peek();
				return currentSlot.page.pageNo != endSlot.page.pageNo || !Util.equals(currentSlot.index, endSlot.index);
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
					KeyPointer kp = slot.getKeyPointer();

					if (kp != null)
						if (kp.pointer instanceof B_Tree.Branch) {
							Page page = loadBranch(slot.page, slot.index);
							currentSlots.push(new Slot(page, 0));
						} else
							return Pair.create(kp.key, kp.getLeafValue());
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
		KeyPointer keyPointer = slot.getKeyPointer();
		Leaf leaf = new Leaf(value);

		if (keyPointer != null && Util.equals(keyPointer.key, key)) {
			keyPointer.pointer = leaf; // Replace existing value
			savePage(slot.page);
		} else
			addAndSplit(slots, new KeyPointer(key, leaf));
	}

	private void addAndSplit(Slots slots, KeyPointer toInsert) {
		boolean done = false;

		// Traversed to deepest. Inserts key-value pair
		do {
			Slot slot = slots.pop();
			Page page = slot.page;
			page.add(slot.index, toInsert);

			int size = page.size();
			done = size <= branchFactor;

			if (!done) { // Splits list into two pages
				int half = branchFactor / 2;
				int pageNo0 = allocator.allocate(), pageNo1 = page.pageNo;
				Page p0 = new Page(pageNo0, page.subList(0, half));
				Page p1 = new Page(pageNo1, page.subList(half, size));
				savePage(p0);
				savePage(p1);

				toInsert = pointerTo(p0); // Propagates to parent

				if (slots.empty()) { // Have to create a new root
					KeyPointer kp = pointerTo(p1);

					create();
					page = new Page(getRoot(), Arrays.asList(toInsert, kp));
					savePage(page);
					done = true;
				}
			} else
				savePage(page);
		} while (!done);
	}

	public void remove(Key key) {
		int root = getRoot();
		Stack<Slot> slots = traverse(key);

		// Remove the entry
		Slot slot = slots.pop();
		Page page = slot.page;
		int index = slot.index;
		KeyPointer keyPointer = slot.getKeyPointer();

		if (keyPointer != null && Util.equals(keyPointer.key, key))
			page.remove(index);
		else
			return;

		// Rotates the tree to maintain balance
		while (page.pageNo != root) {
			int half = branchFactor / 2;
			if (page.size() >= half)
				break;

			Page mp = page;

			slot = slots.pop();
			page = slot.page;
			index = slot.index;

			Page lp = loadBranch(page, index - 1);
			Page rp = loadBranch(page, index + 1);
			int lsize = lp != null ? lp.size() : 0;
			int rsize = rp != null ? rp.size() : 0;

			if (lsize >= rsize && lsize != 0)
				if (lsize > half) { // Shift
					KeyPointer out = lp.remove(lsize - 1);
					mp.add(0, out);
					savePage(mp);
					savePage(lp);
					page.set(index - 1, pointerTo(lp));
				} else
					merge(page, lp, mp, index - 1);
			else if (rsize >= lsize && rsize != 0)
				if (rsize > half) { // Shift
					KeyPointer out = rp.remove(0);
					mp.add(out);
					savePage(mp);
					savePage(rp);
					page.set(index, pointerTo(mp));
				} else
					merge(page, mp, rp, index);
			else {

				// Left/right node empty, should not happen if re-balanced well
				page.clear();
				page.addAll(mp);
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
		p1.addAll(0, p0);
		savePage(p1);
		allocator.deallocate(p0.pageNo);
		parent.remove(index);
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
				pageNo = kp.getBranchPageNo();
			else
				pageNo = null;
		}

		return traversed;
	}

	private int findPosition(Page page, Key key) {
		int i, size = page.size();
		for (i = 0; i < size; i++)
			if (comparator.compare(page.get(i).key, key) >= 0)
				break;
		return i;
	}

	public void dump(PrintStream w, String pfx, int pageNo) {
		Page page = loadPage(pageNo);

		for (KeyPointer kp : page) {
			Pointer ptr = kp.pointer;

			if (ptr instanceof B_Tree.Branch) {
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) ptr;
				dump(w, pfx + "\t", branch.pageNo);
				w.println(pfx + kp.key);
			} else
				w.println(pfx + kp.key + " = " + kp.getLeafValue());
		}
	}

	private Page loadBranch(Page page, int index) {
		KeyPointer keyPointer = getKeyPointer(page, index);

		if (keyPointer != null && keyPointer.pointer instanceof B_Tree.Branch)
			return loadPage(keyPointer.getBranchPageNo());
		else
			return null;
	}

	private Page loadPage(int pageNo) {
		return pagePersister.load(pageNo);
	}

	private void savePage(Page page) {
		pagePersister.save(page.pageNo, page);
	}

	private KeyPointer pointerTo(Page page) {
		Key largest = page.get(page.size() - 1).key;
		return new KeyPointer(largest, new Branch(page.pageNo));
	}

	private KeyPointer getKeyPointer(Page page, Integer index) {
		if (index >= 0 && index < page.size())
			return page.get(index);
		else
			return null;
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
