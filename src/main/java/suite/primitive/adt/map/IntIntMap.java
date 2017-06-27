package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntIntSink;
import suite.primitive.IntIntSource;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.IntIntPair;

/**
 * Map with primitive int key and primitive int value. Integer.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntIntMap {

	private int size;
	private int[] ks;
	private int[] vs;

	public IntIntMap() {
		this(8);
	}

	public IntIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v == IntFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		IntIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(int key, int v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(int key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public IntIntSource source() {
		return source_();
	}

	// public IntIntStreamlet stream() {
	// return new IntIntStreamlet<>(() -> IntIntOutlet.of(source_()));
	// }

	private int put_(int key, int v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v0;
		while ((v0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntIntSource source_() {
		return new IntIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntIntPair pair) {
				int v;
				while ((v = vs[index]) == IntFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, IntFunUtil.EMPTYVALUE);
	}

}
