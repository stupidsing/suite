package suite.adt;

import suite.primitive.PrimitiveFun.Int_Obj;
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
		int mask = ks.length - 1;
		int index = key & mask;
		Object v_;
		while ((v_ = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v_);
	}

	public V put(int key, V v) {
		return cast(putResize(key, v));

	}

	public IntObjSource<V> source() {
		return source_();
	}

	public IntObjStreamlet<V> stream() {
		return new IntObjStreamlet<>(() -> {
			return IntObjOutlet.of(new IntObjSource<V>() {
				private IntObjSource<V> source = source_();
				private IntObjPair<V> pair0 = IntObjPair.of(0, null);

				public boolean source2(IntObjPair<V> pair) {
					boolean b = source.source2(pair0);
					if (b) {
						pair.t0 = pair0.t0;
						pair.t1 = pair0.t1;
					}
					return b;
				}
			});
		});
	}

	private Object putResize(int key, Object v1) {
		int capacity = ks.length;
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

		return put_(key, v1);
	}

	private Object put_(int key, Object v1) {
		int capacity = ks.length;
		int mask = capacity - 1;
		int index = key & mask;
		Object v0;
		while ((v0 = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("Duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntObjSource<V> source_() {
		int capacity = ks.length;
		return new IntObjSource<V>() {
			private int index = 0;

			public boolean source2(IntObjPair<V> pair) {
				boolean b;
				Object v_ = null;
				while ((b = index < capacity) && (v_ = vs[index]) == null)
					index++;
				if (b) {
					pair.t0 = ks[index++];
					pair.t1 = cast(v_);
				}
				return b;
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
