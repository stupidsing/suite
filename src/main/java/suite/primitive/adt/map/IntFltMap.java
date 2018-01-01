package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.IntFltSink;
import suite.primitive.IntFltSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Flt;
import suite.primitive.adt.pair.IntFltPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntFltMap {

	private int size;
	private int[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, IntFltMap> collect(Obj_Int<T> kf0, Obj_Flt<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Flt<T> vf1 = vf0.rethrow();
		return outlet -> {
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
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntFltMap) {
			IntFltMap other = (IntFltMap) object;
			boolean b = size == other.size;
			for (IntObjPair<Float> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntFltSink sink) {
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		IntFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Float> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public float get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(int key, float v) {
		size++;
		float v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		size += ((vs[index] = fun.apply(v0)) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != FltFunUtil.EMPTYVALUE ? 1 : 0);
		rehash();
	}

	public int size() {
		return size;
	}

	public IntFltSource source() {
		return source_();
	}

	public IntObjStreamlet<Float> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Float>() {
			private IntFltSource source0 = source_();
			private IntFltPair pair0 = IntFltPair.of((int) 0, (float) 0);

			public boolean source2(IntObjPair<Float> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private float update_(int index, int key, float v1) {
		float v0 = vs[index];
		ks[index] = key;
		size += ((vs[index] = v1) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != FltFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == FltFunUtil.EMPTYVALUE) {
			int mask = vs.length - 1;
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					if (vs[index1] != FltFunUtil.EMPTYVALUE) {
						int k = ks[index1];
						float v = vs[index1];
						rehash(index1);
						store(k, v);
					}
				}
			}.rehash(index);
		}
		rehash();
		return v0;
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			int[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private float store(int key, float v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
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
				while (index < capacity)
					if ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(ks[index++], v);
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
