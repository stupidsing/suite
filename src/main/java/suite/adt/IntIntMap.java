package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.Int_Int;
import suite.primitive.PrimitiveFun.Source2_IntInt;

/**
 * Map with integer key and positive integer object value. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap {

	private int size;
	private int ks[];
	private int vs[];

	public IntIntMap() {
		this(8);
	}

	public IntIntMap(int capacity) {
		allocate(capacity);
	}

	public int compileIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v < 0)
			put(key, v = fun.apply(key));
		return v;
	}

	public int get(int key) {
		int mask = ks.length - 1;
		int index = key & mask;
		int v;
		while (0 <= (v = vs[index]))
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(int key, int v) {
		return put_(key, v);

	}

	public Source2_IntInt source() {
		int capacity = ks.length;
		return new Source2_IntInt() {
			private int index = 0;

			public boolean source2(IntIntPair pair) {
				boolean b;
				int v = -1;
				while ((b = index < capacity) && (v = vs[index]) < 0)
					index++;
				if (b) {
					pair.t0 = ks[index++];
					pair.t1 = v;
				}
				return b;
			}
		};
	}

	private int put_(int key, int v1) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int ks0[] = ks;
			int vs0[] = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int o = vs0[i];
				if (0 <= o)
					put_(ks0[i], o);
			}
		}

		int mask = capacity - 1;
		int index = key & mask;
		int v0;
		while (0 <= (v0 = vs[index]))
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, -1);
	}

}
