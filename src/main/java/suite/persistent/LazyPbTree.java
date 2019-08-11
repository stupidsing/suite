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
import primal.fp.Funs.Source;
import primal.streamlet.Streamlet;
import suite.streamlet.Read;

public class LazyPbTree<T> implements PerTree<T> {

	private static int maxBranchFactor = 32;
	private static int minBranchFactor = maxBranchFactor / 2;

	private Comparator<T> comparator;
	public final List<Slot<T>> root;

	/**
	 * List<Slot<T>> would be null in leaves. Pivot stores the leaf value.
	 *
	 * Pivot would be null at the minimum side of a tree as the guarding key.
	 */
	public static class Slot<T> {
		public final Source<List<Slot<T>>> source;
		public final T pivot;

		public Slot(Source<List<Slot<T>>> source, T pivot) {
			this.source = source;
			this.pivot = pivot;
		}

		public List<Slot<T>> readSlots() {
			return source.g();
		}
	}

	private class FindSlot {
		private Slot<T> slot;
		private int i, c;

		private FindSlot(List<Slot<T>> slots, T t) {
			this(slots, t, true);
		}

		private FindSlot(List<Slot<T>> slots, T t, boolean isInclusive) {
			i = slots.size();
			while (0 < i)
				if ((c = comparator.compare((slot = slots.get(--i)).pivot, t)) <= 0)
					if (isInclusive || c < 0)
						break;
		}
	}

	public static <T> LazyPbTree<T> of(Comparator<T> comparator, List<T> ts) {
		var list = Read.from(ts).cons(null).map(t -> new Slot<>(null, t)).toList();
		int size;

		while (maxBranchFactor <= (size = list.size())) {
			var list1 = new ArrayList<Slot<T>>();
			for (var i = 0; i < size;) {
				var i0 = i;
				var i1 = i + maxBranchFactor <= size ? i + minBranchFactor : size;
				list1.add(new Slot<>(() -> list.subList(i0, i1), list.get(i).pivot));
				i = i1;
			}
		}

		return new LazyPbTree<>(comparator, list);
	}

	public LazyPbTree(Comparator<T> comparator) {
		this(comparator, List.of(new Slot<T>(() -> List.of(), null)));
	}

	public LazyPbTree(Comparator<T> comparator, List<Slot<T>> source) {
		this.comparator = comparator;
		this.root = source;
	}

	public boolean validate() {
		return Read.from(root).isAll(this::validate) ? true : fail();
	}

	private boolean validate(Slot<T> slot) {
		var slots = slot.readSlots();
		var size = slots.size();
		T p = null;

		var b = size == 0 || true //
				&& (minBranchFactor <= size || failBool("too few branches")) //
				&& (size < maxBranchFactor || failBool("too many branches"));

		for (var slot_ : slots) {
			b = b //
					&& (comparator.compare(slot.pivot, slot_.pivot) <= 0 || failBool("wrong slot")) //
					&& validate(slot_) //
					&& (p == null || comparator.compare(p, slot_.pivot) < 0 || failBool("wrong key order"));
			p = slot_.pivot;
		}

		return b;
	}

	@Override
	public Streamlet<T> streamlet() {
		return stream(null, null);
	}

	public Streamlet<T> stream(T start, T end) {
		return stream_(root, start, end).drop(1).map(slot -> slot.pivot);
	}

	private Streamlet<Slot<T>> stream_(List<Slot<T>> node, T start, T end) {
		var i0 = start != null ? new FindSlot(node, start, false).i : 0;
		var i1 = end != null ? new FindSlot(node, end, false).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(i0, i1)).concatMap(slot -> {
				var slots = slot.readSlots();
				if (!slots.isEmpty())
					return stream_(slots, start, end);
				else
					return Read.each(slot);
			});
		else
			return Read.empty();
	}

	public T find(T t) {
		var node = root;
		FindSlot fs = null;
		while (!node.isEmpty()) {
			fs = new FindSlot(node, t);
			node = fs.slot.readSlots();
		}
		return fs != null && fs.c == 0 ? fs.slot.pivot : null;
	}

	public LazyPbTree<T> add(T t) {
		return update(t, t0 -> t0 == null ? t : fail("duplicate key"));
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace
	 * stored value for the same key.
	 *
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public LazyPbTree<T> replace(T t) {
		return update(t, t_ -> t);
	}

	public LazyPbTree<T> remove(T t) {
		return update(t, t_ -> null);
	}

	public LazyPbTree<T> update(T t, Iterate<T> fun) {
		return new LazyPbTree<>(comparator, newRoot(update(root, t, fun)));
	}

	private List<Slot<T>> update(List<Slot<T>> node0, T t, Iterate<T> fun) {

		// finds appropriate slot
		var fs = new FindSlot(node0, t);
		var size = node0.size();
		int s0 = fs.i, s1 = fs.i + 1;
		var slots0 = fs.slot.readSlots();
		List<Slot<T>> slots2;

		if (!slots0.isEmpty()) { // recurse into branches
			var slots1 = update(slots0, t, fun);
			List<Slot<T>> inner;

			// merges with a neighbor if less than minimum number of nodes
			if (slots1.size() == 1 && (inner = slots1.get(0).readSlots()).size() < minBranchFactor)
				if (0 < s0)
					slots2 = meld(node0.get(--s0).readSlots(), inner);
				else if (s1 < size)
					slots2 = meld(inner, node0.get(s1++).readSlots());
				else
					slots2 = slots1;
			else
				slots2 = slots1;
		} else if (fs.c == 0) { // already exists
			var t1 = fun.apply(fs.slot.pivot);
			slots2 = t1 != null ? List.of(new Slot<>(() -> List.of(), t1)) : List.of();
		} else { // key not exists
			var t1 = fun.apply(null);
			if (t1 != null)
				slots2 = List.of(fs.slot, new Slot<>(() -> List.of(), t1));
			else
				return node0; // no change
		}

		var slots3 = Concat.lists(Left.of(node0, s0), slots2, Right.of(node0, s1));
		List<Slot<T>> node1;

		// checks if need to split
		if (slots3.size() < maxBranchFactor)
			node1 = List.of(slot(slots3));
		else { // splits into two if reached maximum number of nodes
			var leftSlots = Left.of(slots3, minBranchFactor);
			var rightSlots = Right.of(slots3, minBranchFactor);
			node1 = List.of(slot(leftSlots), slot(rightSlots));
		}

		return node1;
	}

	private List<Slot<T>> meld(List<Slot<T>> node0, List<Slot<T>> node1) {
		List<Slot<T>> melded;

		if (maxBranchFactor <= node0.size() + node1.size()) {
			List<Slot<T>> leftSlots, rightSlots;

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

	private List<Slot<T>> newRoot(List<Slot<T>> node) {
		List<Slot<T>> node1;
		return node.size() == 1 && !(node1 = node.get(0).readSlots()).isEmpty() ? newRoot(node1) : node;
	}

	private Slot<T> slot(List<Slot<T>> slots) {
		return new Slot<>(() -> slots, First.of(slots).pivot);
	}

	@Override
	public String toString() {
		return Build.string(sb -> new Object() {
			private void dump(List<Slot<T>> node, String indent) {
				if (node != null)
					for (var slot : node) {
						sb.append(indent + (slot.pivot != null ? slot.pivot : "<-inf>") + "\n");
						dump(slot.readSlots(), indent + "  ");
					}
			}
		}.dump(root, ""));
	}

}
