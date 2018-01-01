package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.FltDblSink;
import suite.primitive.FltDblSource;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Dbl;
import suite.primitive.adt.pair.FltDblPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive float key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltDblMap {

	private int size;
	private float[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, FltDblMap> collect(Obj_Flt<T> kf0, Obj_Dbl<T> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Obj_Dbl<T> vf1 = vf0.rethrow();
		return outlet -> {
			FltDblMap map = new FltDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public FltDblMap() {
		this(8);
	}

	public FltDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(float key, Flt_Dbl fun) {
		double v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltDblMap) {
			FltDblMap other = (FltDblMap) object;
			boolean b = size == other.size;
			for (FltObjPair<Double> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(FltDblSink sink) {
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		FltDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (FltObjPair<Double> pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(float key, double v) {
		size++;
		double v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(float key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		size += ((vs[index] = fun.apply(v0)) != DblFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != DblFunUtil.EMPTYVALUE ? 1 : 0);
		rehash();
	}

	public int size() {
		return size;
	}

	public FltDblSource source() {
		return source_();
	}

	public FltObjStreamlet<Double> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(new FltObjSource<Double>() {
			private FltDblSource source0 = source_();
			private FltDblPair pair0 = FltDblPair.of((float) 0, (double) 0);

			public boolean source2(FltObjPair<Double> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private double update_(int index, float key, double v1) {
		double v0 = vs[index];
		ks[index] = key;
		size += ((vs[index] = v1) != DblFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != DblFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == DblFunUtil.EMPTYVALUE) {
			int mask = vs.length - 1;
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					if (vs[index1] != DblFunUtil.EMPTYVALUE) {
						float k = ks[index1];
						double v = vs[index1];
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
			float[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private double store(float key, double v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltDblSource source_() {
		return new FltDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltDblPair pair) {
				double v;
				while (index < capacity)
					if ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
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
		ks = new float[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
