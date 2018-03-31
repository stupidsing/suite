package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngObjSink;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;

/**
 * Map with generic object key and longacter object value. Long.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjLngMap<K> {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

	private int size;
	private Object[] ks;
	private long[] vs;

	public static <K> ObjLngMap<K> collect(LngObjOutlet<K> outlet) {
		ObjLngMap<K> map = new ObjLngMap<>();
		LngObjPair<K> pair = LngObjPair.of((long) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public ObjLngMap() {
		this(8);
	}

	public ObjLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(K key, Obj_Lng<K> fun) {
		long v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjLngMap) {
			@SuppressWarnings("unchecked")
			ObjLngMap<Object> other = (ObjLngMap<Object>) object;
			boolean b = size == other.size;
			for (LngObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(LngObjSink<K> sink) {
		LngObjPair<K> pair = LngObjPair.of((long) 0, null);
		LngObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(K key) {
		int index = index(key);
		return Objects.equals(ks[index], key) ? vs[index] : EMPTYVALUE;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<K> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public void put(K key, long v) {
		size++;
		store(key, v);
		rehash();
	}

	public void update(K key, Lng_Lng fun) {
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
					long v = vs[index1];
					if (v != EMPTYVALUE) {
						Object k = ks[index1];
						vs[index1] = EMPTYVALUE;
						rehash(index1);
						store(k, v);
					}
				}
			}.rehash(index);

		rehash();
	}

	public int size() {
		return size;
	}

	public LngObjSource<K> source() {
		return source_();
	}

	public LngObjStreamlet<K> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			long[] vs0 = vs;
			long v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(Object key, long v1) {
		int index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(Object key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		while (vs[index] != EMPTYVALUE && !ks[index].equals(key))
			index = index + 1 & mask;
		return index;
	}

	private LngObjSource<K> source_() {
		return new LngObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngObjPair<K> pair) {
				while (index < capacity) {
					Object k = ks[index];
					long v = vs[index++];
					if (v != EMPTYVALUE) {
						pair.update(v, cast(k));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
