package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblDblSink;
import suite.primitive.DblDblSource;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.adt.pair.DblDblPair;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblDblMap {

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private int size;
	private double[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, DblDblMap> collect(Obj_Dbl<T> kf0, Obj_Dbl<T> vf0) {
		Obj_Dbl<T> kf1 = kf0.rethrow();
		Obj_Dbl<T> vf1 = vf0.rethrow();
		return outlet -> {
			DblDblMap map = new DblDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblDblMap() {
		this(8);
	}

	public DblDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(double key, Dbl_Dbl fun) {
		double v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblDblMap) {
			DblDblMap other = (DblDblMap) object;
			boolean b = size == other.size;
			for (DblObjPair<Double> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(DblDblSink sink) {
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		DblDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (DblObjPair<Double> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(double key) {
		int index = index(key);
		double v = vs[index];
		return v != EMPTYVALUE && ks[index] == key ? v : EMPTYVALUE;
	}

	public void put(double key, double v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(double key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = index(key);
		double v0 = vs[index];
		double v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					double v_ = vs[index1];
					if (v_ != EMPTYVALUE) {
						double k = ks[index1];
						vs[index1] = EMPTYVALUE;
						rehash(index1);
						store(k, v_);
					}
				}
			}.rehash(index);
		rehash();
	}

	public int size() {
		return size;
	}

	public DblDblSource source() {
		return source_();
	}

	public DblObjStreamlet<Double> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(new DblObjSource<Double>() {
			private DblDblSource source0 = source_();
			private DblDblPair pair0 = DblDblPair.of((double) 0, (double) 0);

			public boolean source2(DblObjPair<Double> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			double[] ks0 = ks;
			double[] vs0 = vs;
			double v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(double key, double v1) {
		int index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private DblDblSource source_() {
		return new DblDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblDblPair pair) {
				while (index < capacity) {
					double k = ks[index];
					double v = vs[index++];
					if (v != EMPTYVALUE) {
						pair.update(k, v);
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
