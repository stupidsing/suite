package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblLngSink;
import suite.primitive.DblLngSource;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.DblLngPair;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive long value. Long.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblLngMap {

	private int size;
	private double[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, DblLngMap> collect(Obj_Dbl<T> kf0, Obj_Lng<T> vf0) {
		Obj_Dbl<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			DblLngMap map = new DblLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblLngMap() {
		this(8);
	}

	public DblLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(double key, Dbl_Lng fun) {
		long v = get(key);
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblLngMap) {
			DblLngMap other = (DblLngMap) object;
			boolean b = size == other.size;
			for (DblObjPair<Long> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(DblLngSink sink) {
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		DblLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (DblObjPair<Long> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public long get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(double key, long v) {
		size++;
		long v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(double key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		size += ((vs[index] = fun.apply(v0)) != LngFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != LngFunUtil.EMPTYVALUE ? 1 : 0);
		rehash();
	}

	public int size() {
		return size;
	}

	public DblLngSource source() {
		return source_();
	}

	public DblObjStreamlet<Long> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(new DblObjSource<Long>() {
			private DblLngSource source0 = source_();
			private DblLngPair pair0 = DblLngPair.of((double) 0, (long) 0);

			public boolean source2(DblObjPair<Long> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private long update_(int index, double key, long v1) {
		long v0 = vs[index];
		ks[index] = key;
		size += ((vs[index] = v1) != LngFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != LngFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == LngFunUtil.EMPTYVALUE) {
			int mask = vs.length - 1;
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					if (vs[index1] != LngFunUtil.EMPTYVALUE) {
						double k = ks[index1];
						long v = vs[index1];
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
			double[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private long store(double key, long v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblLngSource source_() {
		return new DblLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblLngPair pair) {
				long v;
				while (index < capacity)
					if ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
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
		ks = new double[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
