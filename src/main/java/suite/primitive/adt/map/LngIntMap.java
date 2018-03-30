package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.LngIntSink;
import suite.primitive.LngIntSource;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Int;
import suite.primitive.adt.pair.LngIntPair;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive long key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngIntMap {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private long[] ks;
	private int[] vs;

	public static <T> Fun<Outlet<T>, LngIntMap> collect(Obj_Lng<T> kf0, Obj_Int<T> vf0) {
		Obj_Lng<T> kf1 = kf0.rethrow();
		Obj_Int<T> vf1 = vf0.rethrow();
		return outlet -> {
			LngIntMap map = new LngIntMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngIntMap() {
		this(8);
	}

	public LngIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(long key, Lng_Int fun) {
		int v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngIntMap) {
			LngIntMap other = (LngIntMap) object;
			boolean b = size == other.size;
			for (LngObjPair<Integer> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(LngIntSink sink) {
		LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
		LngIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<Integer> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public int get(long key) {
		int index = index(key);
		int v = vs[index];
		return v != EMPTYVALUE && ks[index] == key ? v : EMPTYVALUE;
	}

	public void put(long key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = index(key);
		int v0 = vs[index];
		int v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					int v_ = vs[index1];
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

	public LngIntSource source() {
		return source_();
	}

	public LngObjStreamlet<Integer> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(new LngObjSource<Integer>() {
			private LngIntSource source0 = source_();
			private LngIntPair pair0 = LngIntPair.of((long) 0, (int) 0);

			public boolean source2(LngObjPair<Integer> pair) {
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
			int[] vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(long key, int v1) {
		int index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private LngIntSource source_() {
		return new LngIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngIntPair pair) {
				int v;
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
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
