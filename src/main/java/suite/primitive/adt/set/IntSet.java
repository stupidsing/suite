package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;

/**
 * Set with intacter elements. Integer.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class IntSet {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private int[] vs;

	public static IntSet intersect(IntSet... sets) {
		return sets[0].streamlet().filter(c -> {
			boolean b = true;
			for (IntSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static IntSet union(IntSet... sets) {
		IntSet set = new IntSet();
		for (IntSet set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public IntSet() {
		this(8);
	}

	public IntSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(int c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int[] vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(int c) {
		return vs[index(c)] == c;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntSet) {
			IntSet other = (IntSet) object;
			boolean b = size == other.size;
			for (int c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(IntSink sink) {
		IntSource source = source_();
		int c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (int c : streamlet())
			h = h * 31 + Integer.hashCode(c);
		return h;
	}

	public IntSource source() {
		return source_();
	}

	public IntStreamlet streamlet() {
		return new IntStreamlet(() -> IntOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(int c) {
		int index = index(c);
		boolean b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(int c) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(c) & mask;
		int c0;
		while ((c0 = vs[index]) != EMPTYVALUE && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private IntSource source_() {
		return new IntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public int source() {
				int v;
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
