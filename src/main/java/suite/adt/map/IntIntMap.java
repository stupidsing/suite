package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.IntIntPair;
import suite.adt.pair.IntObjPair;
import suite.primitive.IntPrimitiveFun.Int_Int;
import suite.primitive.IntPrimitiveSink.IntIntSink;
import suite.primitive.IntPrimitiveSource.IntIntSource;
import suite.primitive.IntPrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;

/**
 * Map with integer key and integer object value. EMPTYVALUE is not allowed in
 * values. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap {

	public final static int EMPTYVALUE = Integer.MIN_VALUE;

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
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of(0, 0);
		IntIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(int key) {
		int mask = kvs.length - 1;
		int index = key & mask;
		long kv;
		int v;
		while ((v = v(kv = kvs[index])) != EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(int key, int v) {
		int capacity = kvs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			long[] kvs0 = kvs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long kv0 = kvs0[i];
				int v_ = v(kv0);
				if (v_ != EMPTYVALUE)
					put_(k(kv0), v_);
			}
		}

		return put_(key, v);
	}

	public void update(int key, Int_Int fun) {
		int mask = kvs.length - 1;
		int index = key & mask;
		long kv;
		int v;
		while ((v = v(kv = kvs[index])) != EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		kvs[index] = fun.apply(v);
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
					boolean b = source.source2(pair0);
					if (b) {
						pair.t0 = pair0.t0;
						pair.t1 = pair0.t1;
					}
					return b;
				}
			});
		});
	}

	private int put_(int key, int v1) {
		int capacity = kvs.length;
		int mask = capacity - 1;
		int index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		kvs[index] = kv(key, v1);
		return v0;
	}

	private IntIntSource source_() {
		return new IntIntSource() {
			private int capacity = kvs.length;
			private int index = 0;

			public boolean source2(IntIntPair pair) {
				long kv;
				int v;
				while ((v = v(kv = kvs[index])) == EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = k(kv);
				pair.t1 = v;
				index++;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		kvs = new long[capacity];
		Arrays.fill(kvs, kv(0, EMPTYVALUE));
	}

	private long kv(int k, int v) {
		return (long) v << 32 | k & 0xFFFFFFFFL;
	}

	private int k(long kv) {
		return (int) kv;
	}

	private int v(long kv) {
		return (int) (kv >> 32);
	}

}
