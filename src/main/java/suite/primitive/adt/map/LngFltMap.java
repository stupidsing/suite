package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.LngFltSink;
import suite.primitive.LngFltSource;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Flt;
import suite.primitive.adt.pair.LngFltPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngFltMap {

	private int size;
	private long[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, LngFltMap> collect(Obj_Lng<T> kf0, Obj_Flt<T> vf0) {
		Obj_Lng<T> kf1 = kf0.rethrow();
		Obj_Flt<T> vf1 = vf0.rethrow();
		return outlet -> {
			LngFltMap map = new LngFltMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngFltMap() {
		this(8);
	}

	public LngFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(long key, Lng_Flt fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngFltMap) {
			LngFltMap other = (LngFltMap) object;
			boolean b = size == other.size;
			for (LngObjPair<Float> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(LngFltSink sink) {
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		LngFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<Float> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public float get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(long key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			long[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(long key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		size += ((vs[index] = fun.apply(v)) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v != FltFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public LngFltSource source() {
		return source_();
	}

	public LngObjStreamlet<Float> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(new LngObjSource<Float>() {
			private LngFltSource source0 = source_();
			private LngFltPair pair0 = LngFltPair.of((long) 0, (float) 0);

			public boolean source2(LngObjPair<Float> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private float put_(long key, float v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
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

	private LngFltSource source_() {
		return new LngFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngFltPair pair) {
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
		ks = new long[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
