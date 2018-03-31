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
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive long value. Long.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblLngMap {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

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
		if (v == EMPTYVALUE)
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
		int index = index(key);
		long v = vs[index];
		return v != EMPTYVALUE && ks[index] == key ? v : EMPTYVALUE;
	}

	public void put(double key, long v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(double key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = index(key);
		long v0 = vs[index];
		long v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					long v_ = vs[index1];
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

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			double[] ks0 = ks;
			long[] vs0 = vs;
			long v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(double key, long v1) {
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

	private DblLngSource source_() {
		return new DblLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblLngPair pair) {
				while (index < capacity) {
					double k = ks[index];
					long v = vs[index++];
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
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
