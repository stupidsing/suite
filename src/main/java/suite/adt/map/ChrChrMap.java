package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.ChrChrPair;
import suite.primitive.ChrChrSink;
import suite.primitive.ChrChrSource;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;

/**
 * Map with character key and char value. Character.MIN_VALUE is not allowed in
 * values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrChrMap {

	private int size;
	private char[] ks;
	private char[] vs;

	public ChrChrMap() {
		this(8);
	}

	public ChrChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(char key, Chr_Chr fun) {
		char v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrChrSink sink) {
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		ChrChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(char key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public ChrChrSource source() {
		return source_();
	}

	// public ChrChrStreamlet stream() {
	// return new ChrChrStreamlet<>(() -> ChrChrOutlet.of(source_()));
	// }

	private char put_(char key, char v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrChrSource source_() {
		return new ChrChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrChrPair pair) {
				char v;
				while ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new char[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
