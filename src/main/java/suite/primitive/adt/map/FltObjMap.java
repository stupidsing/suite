package suite.primitive.adt.map;

import java.util.Objects;

import suite.primitive.FltPrimitives.FltObjSink;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class FltObjMap<V> {

	private int size;
	private float[] ks;
	private Object[] vs;

	public static <V> FltObjMap<V> collect(FltObjOutlet<V> outlet) {
		FltObjMap<V> map = new FltObjMap<>();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public FltObjMap() {
		this(8);
	}

	public FltObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(float key, Flt_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltObjMap) {
			FltObjMap<?> other = (FltObjMap<?>) object;
			boolean b = size == other.size;
			for (FltObjPair<V> pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(FltObjSink<V> sink) {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		FltObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(float key) {
		var index = index(key);
		return ks[index] == key ? cast(vs[index]) : null;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (FltObjPair<V> pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public void put(float key, V v1) {
		size++;
		store(key, v1);
		rehash();
	}

	public void update(float key, Iterate<V> fun) {
		var mask = vs.length - 1;
		var index = index(key);
		V v0 = cast(vs[index]);
		V v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != null ? 1 : 0) - (v0 != null ? 1 : 0);
		if (v1 == null)
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					Object v = vs[index1];
					if (v != null) {
						float k = ks[index1];
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

	public FltObjSource<V> source() {
		return source_();
	}

	public FltObjStreamlet<V> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			float[] ks0 = ks;
			Object[] vs0 = vs;
			Object o;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((o = vs0[i]) != null)
					store(ks0[i], o);
		}
	}

	private void store(float key, Object v1) {
		var index = index(key);
		if (vs[index] == null) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(float key) {
		var mask = vs.length - 1;
		var index = Float.hashCode(key) & mask;
		while (vs[index] != null && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private FltObjSource<V> source_() {
		return new FltObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltObjPair<V> pair) {
				while (index < capacity) {
					float k = ks[index];
					Object v = vs[index++];
					if (v != null) {
						pair.update(k, cast(v));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
