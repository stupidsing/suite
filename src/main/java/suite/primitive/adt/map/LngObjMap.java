package suite.primitive.adt.map;

import java.util.Objects;

import suite.primitive.LngPrimitives.LngObjSink;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class LngObjMap<V> {

	private int size;
	private long[] ks;
	private Object[] vs;

	public static <V> LngObjMap<V> collect(LngObjOutlet<V> outlet) {
		LngObjMap<V> map = new LngObjMap<>();
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public LngObjMap() {
		this(8);
	}

	public LngObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(long key, Lng_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngObjMap) {
			LngObjMap<?> other = (LngObjMap<?>) object;
			boolean b = size == other.size;
			for (LngObjPair<V> pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(LngObjSink<V> sink) {
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		LngObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<V> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public V put(long key, V v1) {
		size++;
		V v0 = cast(store(key, v1));
		rehash();
		return v0;
	}

	public void update(long key, Iterate<V> fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		V v1 = fun.apply(cast(v0));
		ks[index] = key;
		size += ((vs[index] = v1) != null ? 1 : 0) - (v0 != null ? 1 : 0);
		if (v1 == null)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					Object v = vs[index1];
					if (v != null) {
						long k = ks[index1];
						vs[index1] = null;
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

	public LngObjSource<V> source() {
		return source_();
	}

	public LngObjStreamlet<V> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			long[] ks0 = ks;
			Object[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				Object o = vs0[i];
				if (o != null)
					store(ks0[i], o);
			}
		}
	}

	private Object store(long key, Object v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				return Fail.t("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngObjSource<V> source_() {
		return new LngObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngObjPair<V> pair) {
				Object v;
				while (index < capacity)
					if ((v = vs[index]) == null)
						index++;
					else {
						pair.update(ks[index++], cast(v));
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new long[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
