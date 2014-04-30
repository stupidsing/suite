package suite.immutable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class I23Tree<T> implements ITree<T> {

	private int maxBranchFactor = 4;
	private int minBranchFactor = maxBranchFactor / 2;

	private List<Slot> root;
	private Comparator<T> comparator;

	/**
	 * List<Slot> would be null in leaves. Pivot stores the leaf value.
	 * 
	 * Pivot would be null at the minimum side of a tree as the guarding key.
	 */
	private class Slot {
		private List<Slot> slots;
		private T pivot;

		private Slot(List<Slot> slots, T pivot) {
			this.slots = slots;
			this.pivot = pivot;
		}
	}

	private class FindSlot {
		private Slot slot;
		private int i, c;

		private FindSlot(List<Slot> slots, T t) {
			this(slots, t, false);
		}

		private FindSlot(List<Slot> slots, T t, boolean isExclusive) {
			i = slots.size() - 1;
			while ((c = compare((slot = slots.get(i)).pivot, t)) > 0 || isExclusive && c == 0)
				i--;
		}
	}

	public I23Tree(Comparator<T> comparator) {
		this.root = Arrays.asList(new Slot(null, null));
		this.comparator = comparator;
	}

	private I23Tree(Comparator<T> comparator, List<Slot> root) {
		this.root = root;
		this.comparator = comparator;
	}

	@Override
	public Source<T> source() {
		return source(root, null, null);
	}

	private Source<T> source(List<Slot> node, T start, T end) {
		int i0 = start != null ? new FindSlot(node, start).i : 0;
		int i1 = end != null ? new FindSlot(node, end, true).i + 1 : node.size();

		if (i0 < i1)
			return FunUtil.concat(FunUtil.map(new Fun<Slot, Source<T>>() {
				public Source<T> apply(Slot slot) {
					if (slot.slots != null)
						return source(slot.slots, start, end);
					else
						return slot.pivot != null ? To.source(slot.pivot) : FunUtil.<T> nullSource();
				}
			}, To.source(node.subList(i0, i1))));
		else
			return FunUtil.nullSource();
	}

	public T find(T t) {
		List<Slot> node = root;
		FindSlot fs = null;
		while (node != null) {
			fs = new FindSlot(node, t);
			node = fs.slot.slots;
		}
		return fs != null && fs.c == 0 ? fs.slot.pivot : null;
	}

	public I23Tree<T> add(T t) {
		return add(t, false);
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace
	 * stored value for the same key.
	 * 
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public I23Tree<T> replace(T t) {
		return add(t, true);
	}

	public I23Tree<T> remove(T t) {
		return new I23Tree<T>(comparator, createRootNode(remove(root, t)));
	}

	private I23Tree<T> add(T t, boolean isReplace) {
		return new I23Tree<T>(comparator, createRootNode(add(root, t, isReplace)));
	}

	private List<Slot> add(List<Slot> node0, T t, boolean isReplace) {

		// Finds appropriate slot
		FindSlot fs = new FindSlot(node0, t);

		// Adds the node into it
		List<Slot> replaceSlots;

		if (fs.slot.slots != null)
			replaceSlots = add(fs.slot.slots, t, isReplace);
		else if (fs.c != 0)
			replaceSlots = Arrays.asList(fs.slot, new Slot(null, t));
		else if (isReplace)
			replaceSlots = Arrays.asList(new Slot(null, t));
		else
			throw new RuntimeException("Duplicate node " + t);

		List<Slot> slots1 = Util.add(Util.left(node0, fs.i), replaceSlots, Util.right(node0, fs.i + 1));
		List<Slot> node1;

		// Checks if need to split
		if (slots1.size() < maxBranchFactor)
			node1 = Arrays.asList(slot(slots1));
		else { // Splits into two if reached maximum number of nodes
			List<Slot> leftSlots = Util.left(slots1, minBranchFactor);
			List<Slot> rightSlots = Util.right(slots1, minBranchFactor);
			node1 = Arrays.asList(slot(leftSlots), slot(rightSlots));
		}

		return node1;
	}

	private List<Slot> remove(List<Slot> node0, T t) {

		// Finds appropriate slot
		int size = node0.size();
		FindSlot fs = new FindSlot(node0, t);

		// Removes the node from it
		int s0 = fs.i, s1 = fs.i + 1;
		List<Slot> replaceSlots;

		if (fs.slot.slots != null) {
			List<Slot> slots1 = remove(fs.slot.slots, t);

			// Merges with a neighbor if reached minimum number of nodes
			if (slots1.size() < minBranchFactor)
				if (s0 > 0)
					replaceSlots = merge(node0.get(--s0).slots, slots1);
				else if (s1 < size)
					replaceSlots = merge(slots1, node0.get(s1++).slots);
				else
					replaceSlots = Arrays.asList(slot(slots1));
			else
				replaceSlots = Arrays.asList(slot(slots1));
		} else if (fs.c == 0)
			replaceSlots = Collections.emptyList();
		else
			throw new RuntimeException("List<Slot> not found " + t);

		return Util.add(Util.left(node0, s0), replaceSlots, Util.right(node0, s1));
	}

	private List<Slot> merge(List<Slot> node0, List<Slot> node1) {
		List<Slot> merged;

		if (node0.size() + node1.size() >= maxBranchFactor) {
			List<Slot> leftSlots, rightSlots;

			if (node0.size() > minBranchFactor) {
				leftSlots = Util.left(node0, -1);
				rightSlots = Util.add(Arrays.asList(Util.last(node0)), node1);
			} else {
				leftSlots = Util.add(node0, Arrays.asList(Util.first(node1)));
				rightSlots = Util.right(node1, 1);
			}

			merged = Arrays.asList(slot(leftSlots), slot(rightSlots));
		} else
			merged = Arrays.asList(slot(Util.add(node0, node1)));

		return merged;
	}

	private List<Slot> createRootNode(List<Slot> node) {
		List<Slot> node1;
		return node.size() == 1 && (node1 = node.get(0).slots) != null ? node1 : node;
	}

	private Slot slot(List<Slot> slots) {
		return new Slot(slots, Util.first(slots).pivot);
	}

	private int compare(T t0, T t1) {
		boolean b0 = t0 != null;
		boolean b1 = t1 != null;

		if (b0 && b1)
			return comparator.compare(t0, t1);
		else
			return b0 ? 1 : b1 ? -1 : 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		dump(sb, root, "");
		return sb.toString();
	}

	private void dump(StringBuilder sb, List<Slot> node, String indent) {
		if (node != null)
			for (Slot slot : node) {
				sb.append(indent + (slot.pivot != null ? slot.pivot : "<-inf>") + "\n");
				dump(sb, slot.slots, indent + "  ");
			}
	}

}
