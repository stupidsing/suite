package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.FltFltSink;
import suite.primitive.FltFltSource;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.pair.FltFltPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive float key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltFltMap {

	private int size;
	private float[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, FltFltMap> collect(Obj_Flt<T> kf0, Obj_Flt<T> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Obj_Flt<T> vf1 = vf0.rethrow();
		return outlet -> {
			FltFltMap map = new FltFltMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public FltFltMap() {
		this(8);
	}

	public FltFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(float key, Flt_Flt fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(FltFltSink sink) {
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		FltFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(float key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(float key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public int size() {
		return size;
	}

	public FltFltSource source() {
		return source_();
	}

	public FltObjStreamlet<Float> stream() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(new FltObjSource<Float>() {
			private FltFltSource source0 = source_();
			private FltFltPair pair0 = FltFltPair.of((float) 0, (float) 0);

			public boolean source2(FltObjPair<Float> pair) {
				boolean b = source0.source2(pair0);
				pair.t0 = pair0.t0;
				pair.t1 = pair0.t1;
				return b;
			}
		}));
	}

	private float put_(float key, float v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
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

	private FltFltSource source_() {
		return new FltFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltFltPair pair) {
				float v;
				while ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
