package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.CharObjPair;
import suite.primitive.CharPrimitiveFun.Char_Char;
import suite.primitive.CharPrimitiveFun.Obj_Char;
import suite.primitive.CharPrimitiveSink.CharObjSink;
import suite.primitive.CharPrimitiveSource.CharObjSource;
import suite.streamlet.CharObjOutlet;
import suite.streamlet.CharObjStreamlet;

/**
 * Map with generic object key and character object value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class ObjCharMap<K> {

	public final static char EMPTYVALUE = Character.MIN_VALUE;

	private int size;
	private Object[] ks;
	private char[] vs;

	public ObjCharMap() {
		this(8);
	}

	public ObjCharMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Char<K> fun) {
		char v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(CharObjSink<K> sink) {
		CharObjPair<K> pair = CharObjPair.of((char) 0, null);
		CharObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(K key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Char_Char fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public CharObjSource<K> source() {
		return source_();
	}

	public CharObjStreamlet<K> stream() {
		return new CharObjStreamlet<>(() -> CharObjOutlet.of(source_()));
	}

	private char put_(Object key, char v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private CharObjSource<K> source_() {
		return new CharObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(CharObjPair<K> pair) {
				char v;
				while ((v = vs[index]) == EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t1 = cast(ks[index++]);
				pair.t0 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
