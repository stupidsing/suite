package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntLngSink;
import suite.primitive.IntLngSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.IntLngPair;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive long value. Long.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntLngMap {

	private int size;
	private int[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, IntLngMap> collect(Obj_Int<T> kf0, Obj_Lng<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			IntLngMap map = new IntLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntLngMap() {
		this(8);
	}

	public IntLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(int key, Int_Lng fun) {
		long v = get(key);
		if (v == IntFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntLngSink sink) {
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		IntLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(int key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(int key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public IntLngSource source() {
		return source_();
	}

	// public IntLngStreamlet stream() {
	// return new IntLngStreamlet<>(() -> IntLngOutlet.of(source_()));
	// }

	private long put_(int key, long v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntLngSource source_() {
		return new IntLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntLngPair pair) {
				long v;
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
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
