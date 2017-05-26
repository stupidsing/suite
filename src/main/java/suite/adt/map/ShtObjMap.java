package suite.adt.map;

import suite.adt.pair.ShtObjPair;
import suite.primitive.ShtPrimitiveFun.Sht_Obj;
import suite.primitive.ShtPrimitiveSink.ShtObjSink;
import suite.primitive.ShtPrimitiveSource.ShtObjSource;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.streamlet.ShtObjOutlet;
import suite.streamlet.ShtObjStreamlet;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class ShtObjMap<V> {

	private int size;
	private short[] ks;
	private Object[] vs;

	public ShtObjMap() {
		this(8);
	}

	public ShtObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(short key, Sht_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ShtObjSink<V> sink) {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		ShtObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(short key) {
		int mask = vs.length - 1;
		int index = Short.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	public V put(short key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			short[] ks0 = ks;
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

	public void update(short key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = Short.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
	}

	public ShtObjSource<V> source() {
		return source_();
	}

	public ShtObjStreamlet<V> stream() {
		return new ShtObjStreamlet<>(() -> ShtObjOutlet.of(source_()));
	}

	private Object put_(short key, Object v1) {
		int mask = vs.length - 1;
		int index = Short.hashCode(key) & mask;
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

	private ShtObjSource<V> source_() {
		return new ShtObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ShtObjPair<V> pair) {
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
		ks = new short[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
