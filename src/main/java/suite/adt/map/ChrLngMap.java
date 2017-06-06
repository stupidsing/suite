package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.ChrLngPair;
import suite.primitive.ChrLngSink;
import suite.primitive.ChrLngSource;
import suite.primitive.Chr_Lng;
import suite.primitive.Lng_Lng;

/**
 * Map with character key and long value. Long.MIN_VALUE is not allowed in
 * values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrLngMap {

	public final static long EMPTYVALUE = Long.MIN_VALUE;

	private int size;
	private char[] ks;
	private long[] vs;

	public ChrLngMap() {
		this(8);
	}

	public ChrLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(char key, Chr_Lng fun) {
		long v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrLngSink sink) {
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		ChrLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(char key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public ChrLngSource source() {
		return source_();
	}

	// public ChrLngStreamlet stream() {
	// return new ChrLngStreamlet<>(() -> ChrLngOutlet.of(source_()));
	// }

	private long put_(char key, long v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrLngSource source_() {
		return new ChrLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrLngPair pair) {
				long v;
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
		ks = new char[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
