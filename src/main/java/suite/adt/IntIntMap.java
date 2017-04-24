package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.Int_Int;
import suite.primitive.PrimitiveSink.IntIntSink;
import suite.primitive.PrimitiveSource.IntIntSource;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;

/**
 * Map with integer key and integer object value. Integer.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap {

	private int size;
	private long[] kvs;

	public IntIntMap() {
		this(8);
	}

	public IntIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v == Integer.MIN_VALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of(0, 0);
		IntIntSource source = source_();
		while (source.source(pair))
			sink.sink(pair.t0, pair.t1);
	}

	public int get(int key) {
		int mask = kvs.length - 1;
		int index = key & mask;
		long kv;
		int v;
		while ((v = v(kv = kvs[index])) != Integer.MIN_VALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(int key, int v) {
		return putResize(key, v);

	}

	public IntIntSource source() {
		return source_();
	}

	public IntObjStreamlet<Integer> stream() {
		return new IntObjStreamlet<>(() -> {
			IntIntSource source = source_();
			IntIntPair pair0 = IntIntPair.of(0, 0);
			return IntObjOutlet.of(new IntObjSource<Integer>() {
				public boolean source2(IntObjPair<Integer> pair) {
					boolean b = source.source(pair0);
					if (b) {
						pair.t0 = pair0.t0;
						pair.t1 = pair0.t1;
					}
					return b;
				}
			});
		});
	}

	private int putResize(int key, int v1) {
		int capacity = kvs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] kvs0 = kvs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long kv0 = kvs0[i];
				int v = v(kv0);
				if (v != Integer.MIN_VALUE)
					put_(k(kv0), v);
			}
		}

		return put_(key, v1);
	}

	private int put_(int key, int v1) {
		int capacity = kvs.length;
		int mask = capacity - 1;
		int index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != Integer.MIN_VALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("Duplicate key");
		kvs[index] = kv(key, v1);
		return v0;
	}

	private IntIntSource source_() {
		return new IntIntSource() {
			private int capacity = kvs.length;
			private int index = 0;

			public boolean source(IntIntPair pair) {
				boolean b;
				long kv = 0;
				int v = Integer.MIN_VALUE;
				while ((b = index < capacity) && (v = v(kv = kvs[index])) == Integer.MIN_VALUE)
					index++;
				if (b) {
					pair.t0 = k(kv);
					pair.t1 = v;
					index++;
				}
				return b;
			}
		};
	}

	private void allocate(int capacity) {
		kvs = new long[capacity];
		Arrays.fill(kvs, kv(0, Integer.MIN_VALUE));
	}

	private long kv(int k, int v) {
		return Integer.toUnsignedLong(k) + (Integer.toUnsignedLong(v) << 32);
	}

	private int k(long kv) {
		return (int) (kv & 0xFFFFFFFF);
	}

	private int v(long kv) {
		return (int) (kv >> 32);
	}

}
