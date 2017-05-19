package suite.adt.map;

import suite.adt.pair.CharObjPair;
import suite.primitive.CharPrimitiveFun.Char_Obj;
import suite.primitive.CharPrimitiveSink.CharObjSink;
import suite.primitive.CharPrimitiveSource.CharObjSource;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.streamlet.CharObjOutlet;
import suite.streamlet.CharObjStreamlet;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class CharObjMap<V> {

	private int size;
	private char[] ks;
	private Object[] vs;

	public CharObjMap() {
		this(8);
	}

	public CharObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(char key, Char_Obj<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(CharObjSink<V> sink) {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		CharObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink(pair.t0, pair.t1);
	}

	public V get(char key) {
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
		int index = key & mask;
		Object v;
		while ((v = vs[index]) != null)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(cast(v));
	}

	public CharObjSource<V> source() {
		return source_();
	}

	public CharObjStreamlet<V> stream() {
		return new CharObjStreamlet<>(() -> CharObjOutlet.of(source_()));
	}

	private Object put_(char key, Object v1) {
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

	private CharObjSource<V> source_() {
		return new CharObjSource<V>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(CharObjPair<V> pair) {
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
