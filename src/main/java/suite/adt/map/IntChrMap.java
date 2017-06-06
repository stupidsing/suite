package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.IntChrPair;
import suite.primitive.IntChrSink;
import suite.primitive.IntChrSource;
import suite.primitive.Int_Chr;
import suite.primitive.Chr_Chr;

/**
 * Map with intacter key and char value. Character.MIN_VALUE is not allowed in
 * values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntChrMap {

	public final static char EMPTYVALUE = Character.MIN_VALUE;

	private int size;
	private int[] ks;
	private char[] vs;

	public IntChrMap() {
		this(8);
	}

	public IntChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(int key, Int_Chr fun) {
		char v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntChrSink sink) {
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		IntChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(int key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int[] ks0 = ks;
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

	public void update(int key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public IntChrSource source() {
		return source_();
	}

	// public IntObjStreamlet<Character> stream() {
	// return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	// }

	private char put_(int key, char v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntChrSource source_() {
		return new IntChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntChrPair pair) {
				char v;
				while ((v = vs[index]) == EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
