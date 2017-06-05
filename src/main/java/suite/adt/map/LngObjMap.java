package suite.adt.map;

import suite.adt.pair.LngObjPair;
import suite.primitive.LngFun.Lng_Obj;
import suite.primitive.LngSink.LngObjSink;
import suite.primitive.LngSource.LngObjSource;
import suite.primitive.IntFun.Obj_Int;
import suite.streamlet.LngObjOutlet;
import suite.streamlet.LngObjStreamlet;

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

	public V put(long key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] ks0 = ks;
			Object[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				Object o = vs0[i];
				if (o != null)
					put_(ks0[i], o);
			}
		}

		return cast(put_(key, v1));
	}

	public void update(long key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
	}

	public LngObjSource<V> source() {
		return source_();
	}

	public LngObjStreamlet<V> stream() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(source_()));
	}

	private Object put_(long key, Object v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngObjSource<V> source_() {
		return new LngObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngObjPair<V> pair) {
				Object v;
				while ((v = vs[index]) == null)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = cast(v);
				return true;
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
