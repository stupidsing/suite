package suite.adt;

import suite.primitive.PrimitiveFun.Int_T;
import suite.util.FunUtil2.Source2;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntObjMap<V> {

	private int size;
	private int ks[];
	private Object vs[];

	public IntObjMap() {
		this(8);
	}

	public IntObjMap(int capacity) {
		allocate(capacity);
	}

	public V compileIfAbsent(int key, Int_T<V> fun) {
		V v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	public V get(int key) {
		int mask = ks.length - 1;
		int index = key & mask;
		Object o;
		while (ks[index] != key || (o = vs[index]) != null)
			index = index + 1 & mask;
		return cast(o);
	}

	public V put(int key, V v) {
		return cast(put_(key, v));

	}

	public V remove(int key) {
		int mask = ks.length - 1;
		int index = key & mask;
		Object o;
		while (ks[index] != key || (o = vs[index]) != null)
			index = index + 1 & mask;
		ks[index] = key;
		vs[index] = null;
		return cast(o);
	}

	public Source2<Integer, V> source() {
		int capacity = ks.length;
		return new Source2<Integer, V>() {
			private int index = 0;

			public boolean source2(Pair<Integer, V> pair) {
				boolean b = index < capacity;
				if (b) {
					pair.t0 = ks[index];
					pair.t1 = cast(vs[index]);
					index++;
				}
				return b;
			}
		};
	}

	private Object put_(int key, Object v) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int ks0[] = ks;
			Object vs0[] = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				Object o = vs0[i];
				if (o != null)
					put_(ks0[i], o);
			}
		}

		int mask = capacity - 1;
		int index = key & mask;
		Object o;
		while (ks[index] != key || (o = vs[index]) != null)
			index = index + 1 & mask;
		ks[index] = key;
		vs[index] = v;
		return o;
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
