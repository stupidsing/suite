package suite.primitive.adt.map;

import suite.primitive.FltPrimitives.FltObjSink;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;

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

	public void forEach(FltObjSink<V> sink) {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		FltObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	public V put(float key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			float[] ks0 = ks;
			Object[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				Object o = vs0[i];
				if (o != null)
					put_(ks0[i], o);
			}
		}

		return cast(put_(key, v1));
	}

	public void update(float key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
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

	private Object put_(float key, Object v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltObjSource<V> source_() {
		return new FltObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltObjPair<V> pair) {
				Object v;
				while ((v = vs[index]) == null)
					if (capacity <= ++index)
						return false;
				pair.update(ks[index++], cast(v));
				return true;
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
