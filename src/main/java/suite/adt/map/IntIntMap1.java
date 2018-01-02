package suite.adt.map;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntIntSink;
import suite.primitive.IntIntSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.IntIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;

/**
 * Map with integer key and integer object value. EMPTYVALUE is not allowed in
 * values. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap1 {

	private int size;
	private long[] kvs;

	public IntIntMap1() {
		this(8);
	}

	public IntIntMap1(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v == IntFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of(0, 0);
		IntIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntIntMap1) {
			IntIntMap1 other = (IntIntMap1) object;
			boolean b = size == other.size;
			for (IntObjPair<Integer> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public int get(int key) {
		int mask = kvs.length - 1;
		int index = key & mask;
		long kv;
		int v;
		while ((v = v(kv = kvs[index])) != IntFunUtil.EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Integer> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Integer.hashCode(pair.t1);
		}
		return h;
	}

	public int put(int key, int v) {
		size++;
		int v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Int_Int fun) {
		int mask = kvs.length - 1;
		int index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != IntFunUtil.EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		int v1 = fun.apply(v0);
		kvs[index] = kv(key, v1);
		size += (v1 != IntFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != IntFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == IntFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					long kv_ = kvs[index1];
					int v = v(kv_);
					if (v != IntFunUtil.EMPTYVALUE) {
						rehash(index1);
						store(k(kv_), v);
					}
				}
			}.rehash(index);
		rehash();
	}

	public IntIntSource source() {
		return source_();
	}

	public IntObjStreamlet<Integer> streamlet() {
		return new IntObjStreamlet<>(() -> {
			IntIntSource source = source_();
			IntIntPair pair0 = IntIntPair.of(0, 0);
			return IntObjOutlet.of(new IntObjSource<Integer>() {
				public boolean source2(IntObjPair<Integer> pair) {
					boolean b = source.source2(pair0);
					if (b)
						pair.update(pair0.t0, pair0.t1);
					return b;
				}
			});
		});
	}

	private void rehash() {
		int capacity = kvs.length;

		if (capacity * 3 / 4 < size) {
			long[] kvs0 = kvs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long kv0 = kvs0[i];
				int v_ = v(kv0);
				if (v_ != IntFunUtil.EMPTYVALUE)
					store(k(kv0), v_);
			}
		}
	}

	private int store(int key, int v1) {
		int capacity = kvs.length;
		int mask = capacity - 1;
		int index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != IntFunUtil.EMPTYVALUE)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
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
				while (index < capacity)
					if ((v = v(kv = kvs[index])) == IntFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(k(kv), v);
						index++;
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		kvs = new long[capacity];
		Arrays.fill(kvs, kv(0, IntFunUtil.EMPTYVALUE));
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
