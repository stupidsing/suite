package suite.persistent;

import static primal.statics.Fail.fail;
import static primal.statics.Fail.failBool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import primal.Verbs.Build;
import primal.Verbs.Concat;
import primal.Verbs.First;
import primal.Verbs.Last;
import primal.Verbs.Left;
import primal.Verbs.Right;
import primal.fp.Funs.Iterate;
import primal.streamlet.Streamlet;
import suite.streamlet.Read;

public class PbTree<T> implements PerTree<T> {

	private static int maxBranchFactor = 4;
	private static int minBranchFactor = maxBranchFactor / 2;

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

	public static <T> PbTree<T> of(Comparator<T> comparator, List<T> ts) {
		var pbTree = new PbTree<>(comparator);

		var list = Read //
				.from(ts) //
				.cons(null) //
				.map(t -> pbTree.new Slot(null, t)) //
				.toList();

		int size;

		while (maxBranchFactor <= (size = list.size())) {
			var list1 = new ArrayList<>();
			for (var i = 0; i < size;) {
				var i1 = i + maxBranchFactor <= size ? i + minBranchFactor : size;
				list1.add(pbTree.new Slot(list.subList(i, i1), list.get(i).pivot));
				i = i1;
			}
		}

		pbTree.root = list;
		return pbTree;
	}

	public PbTree(Comparator<T> comparator) {
		this.root = List.of(new Slot(null, null));
		this.comparator = comparator;
	}

	private PbTree(Comparator<T> comparator, List<Slot> root) {
		this.root = root;
		this.comparator = comparator;
	}

	public boolean validate() {
		return Read.from(root).isAll(this::validate) ? true : fail();
	}

	private boolean validate(Slot slot) {
		var slots = slot.slots;

		if (slots != null) {
			var size = slots.size();
			T p = null;

			var b = true //
					&& (minBranchFactor <= size || failBool("too few branches")) //
					&& (size < maxBranchFactor || failBool("too many branches"));

			for (var slot_ : slots) {
				b = b //
						&& (comparator.compare(slot.pivot, slot_.pivot) <= 0 || failBool("wrong slot")) //
						&& validate(slot_) //
						&& (p == null || comparator.compare(p, slot_.pivot) < 0 || failBool("wrong key order"));
				p = slot_.pivot;
			}
		}

		return true;
	}

	@Override
	public Streamlet<T> streamlet() {
		return stream(root, null, null);
	}

	private Streamlet<T> stream(List<Slot> node, T start, T end) {
		var i0 = start != null ? new FindSlot(node, start).i : 0;
		var i1 = end != null ? new FindSlot(node, end, true).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(i0, i1)).concatMap(slot -> {
				if (slot.slots != null)
					return stream(slot.slots, start, end);
				else
					return slot.pivot != null ? Read.each(slot.pivot) : Read.empty();
			});
		else
			return Read.empty();
	}

	public T find(T t) {
		var node = root;
		FindSlot fs = null;
		while (node != null) {
			fs = new FindSlot(node, t);
			node = fs.slot.slots;
		}
		return fs != null && fs.c == 0 ? fs.slot.pivot : null;
	}

	public PbTree<T> add(T t) {
		return update(t, t0 -> {
			if (t0 == null)
				return t;
			else
				return fail("duplicate key " + t);
		});
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace stored
	 * value for the same key.
	 *
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public PbTree<T> replace(T t) {
		return update(t, t_ -> t);
	}

	public PbTree<T> remove(T t) {
		return new PbTree<>(comparator, newRoot(update(root, t, t_ -> null)));
	}

	public PbTree<T> update(T t, Iterate<T> fun) {
		return new PbTree<>(comparator, newRoot(update(root, t, fun)));
	}

	private List<Slot> update(List<Slot> node0, T t, Iterate<T> fun) {

		// finds appropriate slot
		var fs = new FindSlot(node0, t);
		var size = node0.size();
		int s0 = fs.i, s1 = fs.i + 1;
		List<Slot> replaceSlots;

		// adds the node into it
		if (fs.slot.slots != null) {
			var slots1 = update(fs.slot.slots, t, fun);
			List<Slot> inner;

			// merges with a neighbor if less than minimum number of nodes
			if (slots1.size() == 1 && (inner = slots1.get(0).slots).size() < minBranchFactor)
				if (0 < s0)
					replaceSlots = meld(node0.get(--s0).slots, inner);
				else if (s1 < size)
					replaceSlots = meld(inner, node0.get(s1++).slots);
				else
					replaceSlots = slots1;
			else
				replaceSlots = slots1;
		} else {
			var t0 = fs.c == 0 ? fs.slot.pivot : null;
			var t1 = fun.apply(t0);

			replaceSlots = new ArrayList<>();
			if (fs.c != 0)
				replaceSlots.add(fs.slot);
			if (t1 != null)
				replaceSlots.add(new Slot(null, t1));
		}

		var slots1 = Concat.lists(Left.of(node0, s0), replaceSlots, Right.of(node0, s1));
		List<Slot> node1;

		// checks if need to split
		if (slots1.size() < maxBranchFactor)
			node1 = List.of(slot(slots1));
		else { // splits into two if reached maximum number of nodes
			var leftSlots = Left.of(slots1, minBranchFactor);
			var rightSlots = Right.of(slots1, minBranchFactor);
			node1 = List.of(slot(leftSlots), slot(rightSlots));
		}

		return node1;
	}

	private List<Slot> meld(List<Slot> node0, List<Slot> node1) {
		List<Slot> melded;

		if (maxBranchFactor <= node0.size() + node1.size()) {
			List<Slot> leftSlots, rightSlots;

			if (minBranchFactor < node0.size()) {
				leftSlots = Left.of(node0, -1);
				rightSlots = Concat.lists(List.of(Last.of(node0)), node1);
			} else if (minBranchFactor < node1.size()) {
				leftSlots = Concat.lists(node0, List.of(First.of(node1)));
				rightSlots = Right.of(node1, 1);
			} else {
				leftSlots = node0;
				rightSlots = node1;
			}

			melded = List.of(slot(leftSlots), slot(rightSlots));
		} else
			melded = List.of(slot(Concat.lists(node0, node1)));

		return melded;
	}

	private List<Slot> newRoot(List<Slot> node) {
		List<Slot> node1;
		return node.size() == 1 && (node1 = node.get(0).slots) != null ? newRoot(node1) : node;
	}

	private Slot slot(List<Slot> slots) {
		return new Slot(slots, First.of(slots).pivot);
	}

	private int compare(T t0, T t1) {
		var b0 = t0 != null;
		var b1 = t1 != null;

		if (b0 && b1)
			return comparator.compare(t0, t1);
		else
			return b0 ? 1 : b1 ? -1 : 0;
	}

	@Override
	public String toString() {
		return Build.string(sb -> new Object() {
			private void dump(List<Slot> node, String indent) {
				if (node != null)
					for (var slot : node) {
						sb.append(indent + (slot.pivot != null ? slot.pivot : "<-inf>") + "\n");
						dump(slot.slots, indent + "  ");
					}
			}
		}.dump(root, ""));
	}

}
