package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngDblSink;
import suite.primitive.LngDblSource;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Dbl;
import suite.primitive.adt.pair.LngDblPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive double value. Double.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngDblMap {

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
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(LngDblSink sink) {
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		LngDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public double get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(long key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			long[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(long key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v;
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

	private double put_(long key, double v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
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
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
