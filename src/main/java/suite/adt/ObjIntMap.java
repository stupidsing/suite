package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSink.IntObjSink;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;

/**
 * Map with generic object key and integer object value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class ObjIntMap<K> {

	private static int EMPTYVALUE = Integer.MIN_VALUE;

	private int size;
	private Object[] ks;
	private int[] vs;

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		int v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.applyAsInt(key));
		return v;
	}

	public void forEach(IntObjSink<K> sink) {
		IntObjPair<K> pair = IntObjPair.of(0, null);
		IntObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink(pair.t0, pair.t1);
	}

	public int get(K key) {
		int mask = ks.length - 1;
		int index = key.hashCode() & mask;
		int v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(K key, int v) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);

	}

	public IntObjSource<K> source() {
		return source_();
	}

	public IntObjStreamlet<K> stream() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	}

	private int put_(Object key, int v1) {
		int capacity = ks.length;
		int mask = capacity - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("Duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntObjSource<K> source_() {
		return new IntObjSource<K>() {
			private int capacity = ks.length;
			private int index = 0;

			public boolean source2(IntObjPair<K> pair) {
				boolean b;
				int v = EMPTYVALUE;
				while ((b = index < capacity) && (v = vs[index]) == EMPTYVALUE)
					index++;
				if (b) {
					pair.t1 = cast(ks[index++]);
					pair.t0 = v;
				}
				return b;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
