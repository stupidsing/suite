package suite.btree.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import suite.btree.Allocator;
import suite.btree.B_Tree;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;

/**
 * B+ tree implementation.
 *
 * @author ywsing
 */
public class B_TreeImpl<Key, Value> implements B_Tree<Key, Value> {

	private int branchFactor;
	private Allocator allocator;
	private SerializedPageFile<Superblock> superblockFile;
	private SerializedPageFile<Bytes> payloadFile;
	private SerializedPageFile<Page> pageFile;
	private Comparator<Key> comparator;

	public class Superblock {
		public int root;
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

	public class KeyPointer {
		public Key key;
		public Pointer pointer;

		public KeyPointer(Key key, Pointer pointer) {
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

		public int getPayloadPageNo() {
			@SuppressWarnings("unchecked")
			Payload payload = (Payload) pointer;
			return payload.pageNo;
		}
	}

	public class Branch implements Pointer {
		private int pageNo;

		public Branch(int pageNo) {
			this.pageNo = pageNo;
		}
	}

	public class Leaf implements Pointer {
		private Value value;

		public Leaf(Value value) {
			this.value = value;
		}
	}

	public class Payload implements Pointer {
		private int pageNo;

		public Payload(int pageNo) {
			this.pageNo = pageNo;
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
			return B_TreeImpl.this.getKeyPointer(page, index);
		}
	}

	private class Traverse {
		private Slots traverse = new Slots();
		private Page page;
		private int index;
		private KeyPointer kp;

		private Traverse(Key key) {
			Integer pageNo = getRoot();

			while (pageNo != null) {
				page = pageFile.load(pageNo);
				index = findPosition(page, key, true);
				kp = getKeyPointer(page, index);
				traverse.push(new Slot(page, index));

				if (kp != null && kp.pointer instanceof B_TreeImpl.Branch)
					pageNo = kp.getBranchPageNo();
				else
					pageNo = null;
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
		int root = allocator.allocate();

		setRoot(root);
		savePage(new Page(root, Arrays.asList(new KeyPointer(null, new Terminal()))));
	}

	@Override
	public Value get(Key key) {
		KeyPointer kp = loadKeyPointer(key);
		return kp != null ? kp.getLeafValue() : null;
	}

	@Override
	public Bytes getPayload(Key key) {
		KeyPointer kp = loadKeyPointer(key);
		return kp != null ? payloadFile.load(kp.getPayloadPageNo()) : null;
	}

	private KeyPointer loadKeyPointer(Key key) {
		KeyPointer kp = new Traverse(key).kp;
		return kp != null && Objects.equals(kp.key, key) ? kp : null;
	}

	@Override
	public Source<Key> keys(Key key0, Key key1) {
		return FunUtil.map(kp -> kp != null ? kp.key : null, source(getRoot(), key0, key1));
	}

	@Override
	public Source<Pair<Key, Value>> range(Key key0, Key key1) {
		return FunUtil.map(kp -> kp != null ? Pair.of(kp.key, kp.getLeafValue()) : null, source(getRoot(), key0, key1));
	}

	private Source<KeyPointer> source(Integer pointer, Key start, Key end) {
		Page page = pageFile.load(pointer);
		int i0 = start != null ? findPosition(page, start, true) : 0;
		int i1 = end != null ? findPosition(page, end, false) + 1 : page.size();

		if (i0 < i1)
			return FunUtil.concat(FunUtil.map(kp -> {
				if (kp.pointer instanceof B_TreeImpl.Branch)
					return source(kp.getBranchPageNo(), start, end);
				else
					return kp.key != null ? To.source(kp) : FunUtil.<KeyPointer> nullSource();
			}, To.source(page.subList(Math.max(0, i0), i1))));
		else
			return FunUtil.nullSource();
	}

	@Override
	public void put(Key key) {
		put(key, new Terminal());
	}

	@Override
	public void put(Key key, Value value) {
		put(key, new Leaf(value));
	}

	@Override
	public void putPayload(Key key, Bytes bytes) {
		int pageNo = allocator.allocate();
		payloadFile.save(pageNo, bytes);
		put(key, new Payload(pageNo));
	}

	private void put(Key key, Pointer pointer) {
		Traverse t = new Traverse(key);
		KeyPointer kp = t.kp;

		if (kp != null && Objects.equals(kp.key, key)) {
			discard(kp);
			kp.pointer = pointer; // Replace existing value
			savePage(t.page);
		} else
			addAndSplit(t.traverse, new KeyPointer(key, pointer));
	}

	private void addAndSplit(Slots slots, KeyPointer toInsert) {
		boolean done;

		// Traversed to deepest. Inserts key-value pair
		do {
			Slot slot = slots.pop();
			Page page = slot.page;
			page.add(slot.index + 1, toInsert);

			int size = page.size();
			done = size <= branchFactor;

			if (!done) { // Splits list into two pages
				int half = branchFactor / 2;
				int pageNo0 = page.pageNo, pageNo1 = allocator.allocate();
				Page p0 = new Page(pageNo0, page.subList(0, half));
				Page p1 = new Page(pageNo1, page.subList(half, size));
				savePage(p0);
				savePage(p1);

				toInsert = pointerTo(p1); // Propagates to parent

				if (slots.empty()) { // Have to create a new root
					KeyPointer kp = pointerTo(p0);

					create();
					page = new Page(getRoot(), Arrays.asList(kp, toInsert));
					savePage(page);
					done = true;
				}
			} else
				savePage(page);
		} while (!done);
	}

	@Override
	public void remove(Key key) {
		int root = getRoot();
		Traverse t = new Traverse(key);
		Stack<Slot> slots = t.traverse;

		// Remove the entry
		Slot slot = slots.pop();
		Page page = slot.page;
		int index = slot.index;
		KeyPointer kp = slot.getKeyPointer();

		if (kp != null && Objects.equals(kp.key, key)) {
			discard(kp);
			page.remove(index);
		} else
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
					page.set(index, pointerTo(mp));
				} else
					merge(page, lp, mp, index - 1);
			else if (rsize >= lsize && rsize != 0)
				if (rsize > half) { // Shift
					KeyPointer out = rp.remove(0);
					mp.add(out);
					savePage(mp);
					savePage(rp);
					page.set(index + 1, pointerTo(rp));
				} else
					merge(page, mp, rp, index);
			else if (slots.size() == 0) {

				// Left/right node empty, should only happen at root node
				page.clear();
				page.addAll(mp);
				savePage(page);
				allocator.deallocate(mp.pageNo);
			} else
				throw new RuntimeException("Unbalanced B-tree");
		}

		savePage(page);
	}

	/**
	 * Merge two consecutive branches in a page.
	 *
	 * p0 and p1 are branches of parent. p0 is located in slot 'index' of
	 * parent, while p1 is in next.
	 */
	private void merge(Page parent, Page p0, Page p1, int index) {
		p0.addAll(p1);
		savePage(p0);
		allocator.deallocate(p1.pageNo);
		parent.remove(index + 1);
	}

	private void dump(PrintStream w, String pfx, int pageNo) {
		Page page = loadPage(pageNo);

		for (KeyPointer kp : page) {
			Pointer ptr = kp.pointer;
			w.print(pfx + (kp.key != null ? kp.key : "MIN-KEY"));

			if (ptr instanceof B_TreeImpl.Branch) {
				w.println();
				@SuppressWarnings("unchecked")
				Branch branch = (Branch) ptr;
				dump(w, pfx + "\t", branch.pageNo);
			} else if (ptr instanceof B_TreeImpl.Leaf)
				w.println(" = " + kp.getLeafValue());
			else if (ptr instanceof B_TreeImpl.Payload)
				w.println(" <Payload>");
			else if (ptr instanceof B_TreeImpl.Terminal)
				w.println(" <>");
		}
	}

	private void discard(KeyPointer kp) {
		if (kp.pointer instanceof B_TreeImpl<?, ?>.Payload)
			allocator.deallocate(kp.getPayloadPageNo());
	}

	private Page loadBranch(Page page, int index) {
		KeyPointer kp = getKeyPointer(page, index);
		return kp != null && kp.pointer instanceof B_TreeImpl.Branch ? loadPage(kp.getBranchPageNo()) : null;
	}

	private Page loadPage(int pageNo) {
		return pageFile.load(pageNo);
	}

	private void savePage(Page page) {
		pageFile.save(page.pageNo, page);
	}

	private KeyPointer pointerTo(Page page) {
		Key smallest = page.get(0).key;
		return new KeyPointer(smallest, new Branch(page.pageNo));
	}

	private int findPosition(Page page, Key key, boolean isInclusive) {
		int i, c;
		for (i = page.size() - 1; i >= 0; i--)
			if ((c = comparator.compare(page.get(i).key, key)) <= 0)
				if (isInclusive || c < 0)
					break;
		return i;
	}

	private KeyPointer getKeyPointer(Page page, Integer index) {
		if (index >= 0 && index < page.size())
			return page.get(index);
		else
			return null;
	}

	@Override
	public void dump(PrintStream w) {
		w.println("==========");
		dump(w, "", getRoot());
	}

	private int getRoot() {
		return superblockFile.load(0).root;
	}

	private void setRoot(int root) {
		Superblock superblock = superblockFile.load(0);
		superblock = superblock != null ? superblock : new Superblock();
		superblock.root = root;
		superblockFile.save(0, superblock);
	}

	public void setBranchFactor(int branchFactor) {
		this.branchFactor = branchFactor;
	}

	public void setAllocator(Allocator allocator) {
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
