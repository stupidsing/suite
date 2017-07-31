package suite.primitive.adt.map;

import suite.primitive.ChrPrimitives.ChrObjSink;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class ChrObjMap<V> {

	private int size;
	private char[] ks;
	private Object[] vs;

	public static <V> ChrObjMap<V> collect(ChrObjOutlet<V> outlet) {
		ChrObjMap<V> map = new ChrObjMap<>();
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ChrObjMap() {
		this(8);
	}

	public ChrObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(char key, Chr_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrObjSink<V> sink) {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		ChrObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return cast(v);
	}

	public V put(char key, V v1) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
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

	public void update(char key, Obj_Int<V> fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
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

	public ChrObjSource<V> source() {
		return source_();
	}

	public ChrObjStreamlet<V> stream() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(source_()));
	}

	private Object put_(char key, Object v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
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

	private ChrObjSource<V> source_() {
		return new ChrObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<V> pair) {
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
		ks = new char[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		V v = (V) o;
		return v;
	}

}
