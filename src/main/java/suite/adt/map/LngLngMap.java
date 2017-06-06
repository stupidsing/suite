package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.LngLngPair;
import suite.primitive.LngLngSink;
import suite.primitive.LngLngSource;
import suite.primitive.Lng_Lng;

/**
 * Map with longacter key and long value. Long.MIN_VALUE is not allowed in
 * values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngLngMap {

	public final static long EMPTYVALUE = Long.MIN_VALUE;

	private int size;
	private long[] ks;
	private long[] vs;

	public LngLngMap() {
		this(8);
	}

	public LngLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(long key, Lng_Lng fun) {
		long v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(LngLngSink sink) {
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		LngLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(long key) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(long key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] ks0 = ks;
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

	public void update(long key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public LngLngSource source() {
		return source_();
	}

	// public LngLngStreamlet stream() {
	// return new LngLngStreamlet<>(() -> LngLngOutlet.of(source_()));
	// }

	private long put_(long key, long v1) {
		int mask = vs.length - 1;
		int index = Long.hashCode(key) & mask;
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

	private LngLngSource source_() {
		return new LngLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngLngPair pair) {
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
		ks = new long[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
