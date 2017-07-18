package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngObjSink;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

/**
 * Map with generic object key and longacter object value. Long.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjLngMap<K> {

	private int size;
	private Object[] ks;
	private long[] vs;

	public static <T, K> Fun<Outlet<T>, ObjLngMap<K>> collect(Fun<T, K> kf0, Obj_Lng<T> vf0) {
		return outlet -> {
			Fun<T, K> kf1 = Rethrow.fun(kf0);
			Obj_Lng<T> vf1 = vf0.rethrow();
			ObjLngMap<K> map = new ObjLngMap<>();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ObjLngMap() {
		this(8);
	}

	public ObjLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(K key, Obj_Lng<K> fun) {
		long v = get(key);
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(LngObjSink<K> sink) {
		LngObjPair<K> pair = LngObjPair.of((long) 0, null);
		LngObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(K key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public LngObjSource<K> source() {
		return source_();
	}

	public LngObjStreamlet<K> stream() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(source_()));
	}

	private long put_(Object key, long v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngObjSource<K> source_() {
		return new LngObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngObjPair<K> pair) {
				long v;
				while ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t1 = cast(ks[index++]);
				pair.t0 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
