package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.LngFunUtil;
import suite.primitive.LngLngSink;
import suite.primitive.LngLngSource;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.LngLngPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive long value. Long.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngLngMap {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

	private int size;
	private long[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, LngLngMap> collect(Obj_Lng<T> kf0, Obj_Lng<T> vf0) {
		Obj_Lng<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			LngLngMap map = new LngLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngLngMap() {
		this(8);
	}

	public LngLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(long key, Lng_Lng fun) {
		long v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngLngMap) {
			LngLngMap other = (LngLngMap) object;
			boolean b = size == other.size;
			for (LngObjPair<Long> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(LngLngSink sink) {
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		LngLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<Long> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public long get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(long key, long v) {
		size++;
		long v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		long v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					long v_ = vs[index1];
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

	public LngLngSource source() {
		return source_();
	}

	public LngObjStreamlet<Long> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(new LngObjSource<Long>() {
			private LngLngSource source0 = source_();
			private LngLngPair pair0 = LngLngPair.of((long) 0, (long) 0);

			public boolean source2(LngObjPair<Long> pair) {
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
			long[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private long store(long key, long v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				Fail.t("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngLngSource source_() {
		return new LngLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngLngPair pair) {
				long v;
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
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
