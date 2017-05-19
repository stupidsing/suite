package suite.adt.map;

import suite.adt.pair.IntObjPair;
import suite.primitive.IntPrimitiveFun.Int_Obj;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSink.IntObjSink;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntObjMap<V> {

	private int size;
	private int[] ks;
	private Object[] vs;

	public IntObjMap() {
		this(8);
	}

	public IntObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(int key, Int_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntObjSink<V> sink) {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		IntObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink(pair.t0, pair.t1);
	}

	public V get(int key) {
		int mask = vs.length - 1;
		int index = key & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	public V put(int key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] ks0 = ks;
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

	public void update(int key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = key & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
	}

	public IntObjSource<V> source() {
		return source_();
	}

	public IntObjStreamlet<V> stream() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	}

	private Object put_(int key, Object v1) {
		int mask = vs.length - 1;
		int index = key & mask;
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

	private IntObjSource<V> source_() {
		return new IntObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntObjPair<V> pair) {
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
		ks = new int[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
