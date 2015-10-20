package suite.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class LazyIbTree<T> implements ITree<T> {

	private int maxBranchFactor = 32;
	private int minBranchFactor = maxBranchFactor / 2;
	private Comparator<T> comparator;

	public final Source<List<Slot<T>>> source;

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
			return source.source();
		}
	}

	private class FindSlot {
		private Slot<T> slot;
		private int i, c;

		private FindSlot(List<Slot<T>> slots, T t) {
			this(slots, t, false);
		}

		private FindSlot(List<Slot<T>> slots, T t, boolean isExclusive) {
			i = slots.size() - 1;
			while ((c = compare((slot = slots.get(i)).pivot, t)) > 0 || isExclusive && c == 0)
				i--;
		}
	}

	public LazyIbTree(Comparator<T> comparator) {
		this.source = () -> Arrays.asList(new Slot<>(null, null));
		this.comparator = comparator;
	}

	public LazyIbTree(Comparator<T> comparator, Source<List<Slot<T>>> source) {
		this.source = source;
		this.comparator = comparator;
	}

	public void validate() {
		Read.from(source.source()).sink(slot -> validate(slot.source));
	}

	private void validate(Source<List<Slot<T>>> source) {
		if (source != null) {
			List<Slot<T>> slots = source.source();
			int size = slots.size();
			T p = null;

			if (size < minBranchFactor)
				throw new RuntimeException("Too few branches");
			else if (size >= maxBranchFactor)
				throw new RuntimeException("Too many branches");

			for (Slot<T> slot_ : slots) {
				validate(slot_.source);
				if (p != null && !(comparator.compare(p, slot_.pivot) < 0))
					throw new RuntimeException("Wrong key order");
				p = slot_.pivot;
			}
		}
	}

	@Override
	public Streamlet<T> stream() {
		return stream(root(), null, null);
	}

	private Streamlet<T> stream(List<Slot<T>> node, T start, T end) {
		int i0 = start != null ? new FindSlot(node, start).i : 0;
		int i1 = end != null ? new FindSlot(node, end, true).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(i0, i1)).concatMap(slot -> {
				if (slot.source != null)
					return stream(slot.readSlots(), start, end);
				else
					return slot.pivot != null ? Read.from(slot.pivot) : Read.empty();
			});
		else
			return Read.empty();
	}

	public T find(T t) {
		List<Slot<T>> node = root();
		FindSlot fs = null;
		while (node != null) {
			fs = new FindSlot(node, t);
			node = fs.slot.readSlots();
		}
		return fs != null && fs.c == 0 ? fs.slot.pivot : null;
	}

	public LazyIbTree<T> add(T t) {
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
	public LazyIbTree<T> replace(T t) {
		return update(t, t_ -> t);
	}

	public LazyIbTree<T> remove(T t) {
		return update(t, t_ -> null);
	}

	public List<Slot<T>> root() {
		return source.source();
	}

	private LazyIbTree<T> update(T t, Fun<T, T> fun) {
		return new LazyIbTree<>(comparator, createRoot(update(root(), t, fun)));
	}

	private List<Slot<T>> update(List<Slot<T>> node0, T t, Fun<T, T> fun) {

		// Finds appropriate slot
		FindSlot fs = new FindSlot(node0, t);
		int size = node0.size();
		int s0 = fs.i, s1 = fs.i + 1;
		List<Slot<T>> replaceSlots;

		// Adds the node into it
		if (fs.slot.source != null) {
			List<Slot<T>> slots1 = update(fs.slot.readSlots(), t, fun);
			List<Slot<T>> inner;

			// Merges with a neighbor if less than minimum number of nodes
			if (slots1.size() == 1 && (inner = slots1.get(0).readSlots()).size() < minBranchFactor)
				if (s0 > 0)
					replaceSlots = merge(node0.get(--s0).readSlots(), inner);
				else if (s1 < size)
					replaceSlots = merge(inner, node0.get(s1++).readSlots());
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
				replaceSlots.add(new Slot<>(null, t1));
		}

		List<Slot<T>> slots1 = Util.add(Util.left(node0, s0), replaceSlots, Util.right(node0, s1));
		List<Slot<T>> node1;

		// Checks if need to split
		if (slots1.size() < maxBranchFactor)
			node1 = Arrays.asList(slot(slots1));
		else { // Splits into two if reached maximum number of nodes
			List<Slot<T>> leftSlots = Util.left(slots1, minBranchFactor);
			List<Slot<T>> rightSlots = Util.right(slots1, minBranchFactor);
			node1 = Arrays.asList(slot(leftSlots), slot(rightSlots));
		}

		return node1;
	}

	private List<Slot<T>> merge(List<Slot<T>> node0, List<Slot<T>> node1) {
		List<Slot<T>> merged;

		if (node0.size() + node1.size() >= maxBranchFactor) {
			List<Slot<T>> leftSlots, rightSlots;

			if (node0.size() > minBranchFactor) {
				leftSlots = Util.left(node0, -1);
				rightSlots = Util.add(Arrays.asList(Util.last(node0)), node1);
			} else if (node1.size() > minBranchFactor) {
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

	private Source<List<Slot<T>>> createRoot(List<Slot<T>> node) {
		List<Slot<T>> node1;
		return node.size() == 1 && (node1 = node.get(0).readSlots()) != null ? createRoot(node1) : () -> node;
	}

	private Slot<T> slot(List<Slot<T>> slots) {
		return new Slot<>(() -> slots, Util.first(slots).pivot);
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
		dump(sb, root(), "");
		return sb.toString();
	}

	private void dump(StringBuilder sb, List<Slot<T>> node, String indent) {
		if (node != null)
			for (Slot<T> slot : node) {
				sb.append(indent + (slot.pivot != null ? slot.pivot : "<-inf>") + "\n");
				dump(sb, slot.readSlots(), indent + "  ");
			}
	}

}
