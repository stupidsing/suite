package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.IntFltSink;
import suite.primitive.IntFltSource;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.IntFltPair;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive float value. Float.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntFltMap {

	private int size;
	private int[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, IntFltMap> collect(Obj_Int<T> kf0, Obj_Flt<T> vf0) {
		return outlet -> {
			Obj_Int<T> kf1 = kf0.rethrow();
			Obj_Flt<T> vf1 = vf0.rethrow();
			IntFltMap map = new IntFltMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntFltMap() {
		this(8);
	}

	public IntFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(int key, Int_Flt fun) {
		float v = get(key);
		if (v == IntFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntFltSink sink) {
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		IntFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(int key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(int key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public IntFltSource source() {
		return source_();
	}

	// public IntFltStreamlet stream() {
	// return new IntFltStreamlet<>(() -> IntFltOutlet.of(source_()));
	// }

	private float put_(int key, float v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntFltSource source_() {
		return new IntFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntFltPair pair) {
				float v;
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
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
