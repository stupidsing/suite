package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.LngChrPair;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.LngChrSink;
import suite.primitive.LngChrSource;
import suite.primitive.LngFunUtil;
import suite.primitive.Lng_Chr;

/**
 * Map with primitive long key and primitive char value. Character.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngChrMap {

	private int size;
	private long[] ks;
	private char[] vs;

	public LngChrMap() {
		this(8);
	}

	public LngChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(long key, Lng_Chr fun) {
		char v = get(key);
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(LngChrSink sink) {
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		LngChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(long key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(long key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public LngChrSource source() {
		return source_();
	}

	// public LngChrStreamlet stream() {
	// return new LngChrStreamlet<>(() -> LngChrOutlet.of(source_()));
	// }

	private char put_(long key, char v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngChrSource source_() {
		return new LngChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngChrPair pair) {
				char v;
				while ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new long[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
