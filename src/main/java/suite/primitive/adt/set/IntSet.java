package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.streamlet.IntOutlet;
import suite.streamlet.IntStreamlet;

/**
 * Set with intacter elements. Integer.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class IntSet {

	private int size;
	private int[] vs;

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

	public boolean add(int v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(v);
	}

	public IntSource source() {
		return source_();
	}

	public IntStreamlet stream() {
		return new IntStreamlet(() -> IntOutlet.of(source_()));
	}

	private boolean add_(int v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(v1) & mask;
		int v0;
		while ((v0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (v0 != v1)
				index = index + 1 & mask;
			else
				return false;
		vs[index] = v1;
		return true;
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
