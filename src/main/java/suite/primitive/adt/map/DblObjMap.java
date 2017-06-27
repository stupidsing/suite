package suite.primitive.adt.map;

import suite.primitive.DblPrimitives.DblObjSink;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.DblObjOutlet;
import suite.streamlet.DblObjStreamlet;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class DblObjMap<V> {

	private int size;
	private double[] ks;
	private Object[] vs;

	public DblObjMap() {
		this(8);
	}

	public DblObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(double key, Dbl_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblObjSink<V> sink) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		DblObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	public V put(double key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
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

	public void update(double key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
	}

	public DblObjSource<V> source() {
		return source_();
	}

	public DblObjStreamlet<V> stream() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(source_()));
	}

	private Object put_(double key, Object v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
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

	private DblObjSource<V> source_() {
		return new DblObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblObjPair<V> pair) {
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
		ks = new double[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
