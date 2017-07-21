package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.FltDblSink;
import suite.primitive.FltDblSource;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Dbl;
import suite.primitive.adt.pair.FltDblPair;
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
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(FltDblSink sink) {
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		FltDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public double get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(float key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(float key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public FltDblSource source() {
		return source_();
	}

	// public FltDblStreamlet stream() {
	// return new FltDblStreamlet<>(() -> FltDblOutlet.of(source_()));
	// }

	private double put_(float key, double v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
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
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
