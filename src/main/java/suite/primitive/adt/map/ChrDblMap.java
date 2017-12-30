package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrDblSink;
import suite.primitive.ChrDblSource;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Dbl;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.adt.pair.ChrDblPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrDblMap {

	private int size;
	private char[] ks;
	private double[] vs;

	public static <T> Fun<Outlet<T>, ChrDblMap> collect(Obj_Chr<T> kf0, Obj_Dbl<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Dbl<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrDblMap map = new ChrDblMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrDblMap() {
		this(8);
	}

	public ChrDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(char key, Chr_Dbl fun) {
		double v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrDblMap) {
			ChrDblMap other = (ChrDblMap) object;
			boolean b = size == other.size;
			for (ChrObjPair<Double> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(ChrDblSink sink) {
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		ChrDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (ChrObjPair<Double> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(char key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			char[] ks0 = ks;
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

	public void update(char key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
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

	public ChrDblSource source() {
		return source_();
	}

	public ChrObjStreamlet<Double> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Double>() {
			private ChrDblSource source0 = source_();
			private ChrDblPair pair0 = ChrDblPair.of((char) 0, (double) 0);

			public boolean source2(ChrObjPair<Double> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private double put_(char key, double v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
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

	private ChrDblSource source_() {
		return new ChrDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrDblPair pair) {
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
		ks = new char[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
