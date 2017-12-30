package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntDblSink;
import suite.primitive.IntDblSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.adt.pair.IntDblPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntDblMap {

	private int size;
	private int[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, IntDblMap> collect(Obj_Int<T> kf0, Obj_Dbl<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Dbl<T> vf1 = vf0.rethrow();
		return outlet -> {
			IntDblMap map = new IntDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntDblMap() {
		this(8);
	}

	public IntDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(int key, Int_Dbl fun) {
		double v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntDblMap) {
			IntDblMap other = (IntDblMap) object;
			boolean b = size == other.size;
			for (IntObjPair<Double> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntDblSink sink) {
		IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
		IntDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Double> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(int key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (IntObjPair<Double> pair : streamlet())
			sb.append(pair.t0 + ":" + pair.t1 + ",");
		return sb.toString();
	}

	public void update(int key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		size += ((vs[index] = fun.apply(v)) != DblFunUtil.EMPTYVALUE ? 1 : 0) - (v != DblFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public IntDblSource source() {
		return source_();
	}

	public IntObjStreamlet<Double> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Double>() {
			private IntDblSource source0 = source_();
			private IntDblPair pair0 = IntDblPair.of((int) 0, (double) 0);

			public boolean source2(IntObjPair<Double> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private double put_(int key, double v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
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

	private IntDblSource source_() {
		return new IntDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntDblPair pair) {
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
		ks = new int[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
