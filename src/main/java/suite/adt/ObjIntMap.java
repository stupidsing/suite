package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSink.IntObjSink2;
import suite.primitive.PrimitiveSource.IntObjSource2;
import suite.streamlet.IntObjOutlet2;
import suite.streamlet.IntObjStreamlet2;

/**
 * Map with generic object key and integer object value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class ObjIntMap<K> {

	private int size;
	private Object ks[];
	private int vs[];

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		int v = get(key);
		if (v == Integer.MIN_VALUE)
			put(key, v = fun.applyAsInt(key));
		return v;
	}

	public void forEach(IntObjSink2<K> sink) {
		IntObjPair<K> pair = IntObjPair.of(0, null);
		IntObjSource2<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(K key) {
		int mask = ks.length - 1;
		int index = key.hashCode() & mask;
		int v_;
		while ((v_ = vs[index]) != Integer.MIN_VALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v_;
	}

	public int put(K key, int v) {
		return put_(key, v);

	}

	public IntObjSource2<K> source() {
		return source_();
	}

	public IntObjStreamlet2<K> stream() {
		return new IntObjStreamlet2<>(() -> {
			return IntObjOutlet2.of(new IntObjSource2<K>() {
				private IntObjSource2<K> source = source_();
				private IntObjPair<K> pair0 = IntObjPair.of(0, null);

				public boolean source2(IntObjPair<K> pair) {
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

	private int put_(Object key, int v1) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object ks0[] = ks;
			int vs0[] = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != Integer.MIN_VALUE)
					put_(ks0[i], v_);
			}
		}

		int mask = capacity - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != Integer.MIN_VALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntObjSource2<K> source_() {
		int capacity = ks.length;
		return new IntObjSource2<K>() {
			private int index = 0;

			public boolean source2(IntObjPair<K> pair) {
				boolean b;
				int v_ = Integer.MIN_VALUE;
				while ((b = index < capacity) && (v_ = vs[index]) == Integer.MIN_VALUE)
					index++;
				if (b) {
					pair.t1 = cast(ks[index++]);
					pair.t0 = v_;
				}
				return b;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, Integer.MIN_VALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
