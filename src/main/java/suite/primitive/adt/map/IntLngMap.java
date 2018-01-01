package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.IntLngSink;
import suite.primitive.IntLngSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.IntLngPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive long value. Long.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntLngMap {

	private int size;
	private int[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, IntLngMap> collect(Obj_Int<T> kf0, Obj_Lng<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			IntLngMap map = new IntLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntLngMap() {
		this(8);
	}

	public IntLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(int key, Int_Lng fun) {
		long v = get(key);
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntLngMap) {
			IntLngMap other = (IntLngMap) object;
			boolean b = size == other.size;
			for (IntObjPair<Long> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntLngSink sink) {
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		IntLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Long> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public long get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(int key, long v) {
		size++;
		long v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		long v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != LngFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != LngFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == LngFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					long v_ = vs[index1];
					if (v_ != LngFunUtil.EMPTYVALUE) {
						int k = ks[index1];
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

	public IntLngSource source() {
		return source_();
	}

	public IntObjStreamlet<Long> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Long>() {
			private IntLngSource source0 = source_();
			private IntLngPair pair0 = IntLngPair.of((int) 0, (long) 0);

			public boolean source2(IntObjPair<Long> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			int[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private long store(int key, long v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
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

	private IntLngSource source_() {
		return new IntLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntLngPair pair) {
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
		ks = new int[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
