package suite.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class IbTree<T> implements ITree<T> {

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
			while (0 < (c = compare((slot = slots.get(i)).pivot, t)) || isExclusive && c == 0)
				i--;
		}
	}

	public IbTree(Comparator<T> comparator) {
		this.root = Arrays.asList(new Slot(null, null));
		this.comparator = comparator;
	}

	private IbTree(Comparator<T> comparator, List<Slot> root) {
		this.root = root;
		this.comparator = comparator;
	}

	public void validate() {
		Read.from(root).sink(this::validate);
	}

	private void validate(Slot slot) {
		List<Slot> slots = slot.slots;

		if (slots != null) {
			int size = slots.size();
			T p = null;

			if (size < minBranchFactor)
				throw new RuntimeException("Too few branches");
			else if (maxBranchFactor <= size)
				throw new RuntimeException("Too many branches");

			for (Slot slot_ : slots) {
				validate(slot_);
				if (p != null && !(comparator.compare(p, slot_.pivot) < 0))
					throw new RuntimeException("Wrong key order");
				p = slot_.pivot;
			}
		}
	}

	@Override
	public Streamlet<T> stream() {
		return stream(root, null, null);
	}

	private Streamlet<T> stream(List<Slot> node, T start, T end) {
		int i0 = start != null ? new FindSlot(node, start).i : 0;
		int i1 = end != null ? new FindSlot(node, end, true).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(i0, i1)).concatMap(slot -> {
				if (slot.slots != null)
					return stream(slot.slots, start, end);
				else
					return slot.pivot != null ? Read.from(slot.pivot) : Read.empty();
			});
		else
			return Read.empty();
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

	public IbTree<T> add(T t) {
		return update(t, t0 -> {
			if (t0 == null)
				return t;
			else
				throw new RuntimeException("Duplicate key");
		});
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace
	 * stored value for the same key.
	 *
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public IbTree<T> replace(T t) {
		return update(t, t_ -> t);
	}

	public IbTree<T> remove(T t) {
		return new IbTree<>(comparator, createRoot(update(root, t, t_ -> null)));
	}

	public IbTree<T> update(T t, Fun<T, T> fun) {
		return new IbTree<>(comparator, createRoot(update(root, t, fun)));
	}

	private List<Slot> update(List<Slot> node0, T t, Fun<T, T> fun) {

		// Finds appropriate slot
		FindSlot fs = new FindSlot(node0, t);
		int size = node0.size();
		int s0 = fs.i, s1 = fs.i + 1;
		List<Slot> replaceSlots;

		// Adds the node into it
		if (fs.slot.slots != null) {
			List<Slot> slots1 = update(fs.slot.slots, t, fun);
			List<Slot> inner;

			// Merges with a neighbor if less than minimum number of nodes
			if (slots1.size() == 1 && (inner = slots1.get(0).slots).size() < minBranchFactor)
				if (0 < s0)
					replaceSlots = merge(node0.get(--s0).slots, inner);
				else if (s1 < size)
					replaceSlots = merge(inner, node0.get(s1++).slots);
				else
					replaceSlots = slots1;
			else
				replaceSlots = slots1;
		} else {
			T t0 = fs.c == 0 ? fs.slot.pivot : null;
			T t1 = fun.apply(t0);

			replaceSlots = new ArrayList<>();
			if (fs.c != 0)
				replaceSlots.add(fs.slot);
			if (t1 != null)
				replaceSlots.add(new Slot(null, t1));
		}

		List<Slot> slots1 = Util.add(Util.left(node0, s0), replaceSlots, Util.right(node0, s1));
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

	private List<Slot> merge(List<Slot> node0, List<Slot> node1) {
		List<Slot> merged;

		if (maxBranchFactor <= node0.size() + node1.size()) {
			List<Slot> leftSlots, rightSlots;

			if (minBranchFactor < node0.size()) {
				leftSlots = Util.left(node0, -1);
				rightSlots = Util.add(Arrays.asList(Util.last(node0)), node1);
			} else if (minBranchFactor < node1.size()) {
				leftSlots = Util.add(node0, Arrays.asList(Util.first(node1)));
				rightSlots = Util.right(node1, 1);
			} else {
				leftSlots = node0;
				rightSlots = node1;
			}

			merged = Arrays.asList(slot(leftSlots), slot(rightSlots));
		} else
			merged = Arrays.asList(slot(Util.add(node0, node1)));

		return merged;
	}

	private List<Slot> createRoot(List<Slot> node) {
		List<Slot> node1;
		return node.size() == 1 && (node1 = node.get(0).slots) != null ? createRoot(node1) : node;
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
