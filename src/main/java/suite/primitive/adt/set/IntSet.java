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

	public void forEach(IntSink sink) {
		IntSource source = source_();
		int c;
		while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	public boolean add(int c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(c);
	}

	public boolean contains(int c) {
		return 0 <= index(c);
	}

	public IntSource source() {
		return source_();
	}

	public IntStreamlet streamlet() {
		return new IntStreamlet(() -> IntOutlet.of(source_()));
	}

	private boolean add_(int c) {
		int index = index(c);
		if (0 <= index) {
			vs[index] = c;
			return true;
		} else
			return false;
	}

	private int index(int c) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(c) & mask;
		int c0;
		while ((c0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (c0 != c)
				index = index + 1 & mask;
			else
				return -1;
		return index;
	}

	private IntSource source_() {
		return new IntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public int source() {
				int v;
				while ((v = vs[index]) == IntFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return IntFunUtil.EMPTYVALUE;
				return v;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new int[capacity];
		Arrays.fill(vs, IntFunUtil.EMPTYVALUE);
	}

}
