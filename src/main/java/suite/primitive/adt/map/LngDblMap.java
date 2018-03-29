package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngDblSink;
import suite.primitive.LngDblSource;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Dbl;
import suite.primitive.adt.pair.LngDblPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngDblMap {

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private int size;
	private long[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, LngDblMap> collect(Obj_Lng<T> kf0, Obj_Dbl<T> vf0) {
		Obj_Lng<T> kf1 = kf0.rethrow();
		Obj_Dbl<T> vf1 = vf0.rethrow();
		return outlet -> {
			LngDblMap map = new LngDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngDblMap() {
		this(8);
	}

	public LngDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(long key, Lng_Dbl fun) {
		double v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngDblMap) {
			LngDblMap other = (LngDblMap) object;
			boolean b = size == other.size;
			for (LngObjPair<Double> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(LngDblSink sink) {
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		LngDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<Double> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(long key, double v) {
		size++;
		double v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		double v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					double v_ = vs[index1];
					if (v_ != EMPTYVALUE) {
						long k = ks[index1];
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

	public LngDblSource source() {
		return source_();
	}

	public LngObjStreamlet<Double> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(new LngObjSource<Double>() {
			private LngDblSource source0 = source_();
			private LngDblPair pair0 = LngDblPair.of((long) 0, (double) 0);

			public boolean source2(LngObjPair<Double> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			long[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private double store(long key, double v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				Fail.t("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngDblSource source_() {
		return new LngDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngDblPair pair) {
				double v;
				while (index < capacity)
					if ((v = vs[index]) == EMPTYVALUE)
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
		vs = new double[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
