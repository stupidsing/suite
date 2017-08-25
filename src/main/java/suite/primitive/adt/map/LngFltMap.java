package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.LngFltSink;
import suite.primitive.LngFltSource;
import suite.primitive.LngFunUtil;
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
 * Map with primitive long key and primitive float value. Float.MIN_VALUE is not
 * allowed in values. Not thread-safe.
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
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(LngFltSink sink) {
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		LngFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
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
			int capacity1 = capacity * 2;
			long[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(long key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
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
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
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
				while ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.update(ks[index++], v);
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new long[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
