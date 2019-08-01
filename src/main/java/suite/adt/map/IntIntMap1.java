package suite.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;

import primal.primitive.IntIntSink;
import primal.primitive.IntIntSource;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.adt.pair.IntIntPair;
import primal.primitive.adt.pair.IntObjPair;
import suite.primitive.Int_Int;
import suite.primitive.streamlet.IntObjPuller;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.primitive.streamlet.IntPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.As;

/**
 * Map with integer key and integer object value. EMPTYVALUE is not allowed in
 * values. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap1 {

	private static int empty = IntPrim.EMPTYVALUE;

	private int size;
	private long[] kvs;

	public IntIntMap1() {
		this(8);
	}

	public IntIntMap1(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		var pair = IntIntPair.of(0, 0);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntIntMap1) {
			var other = (IntIntMap1) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k) == pair.v;
			return b;
		} else
			return false;
	}

	public int get(int key) {
		var mask = kvs.length - 1;
		var index = key & mask;
		long kv;
		int v;
		while ((v = v(kv = kvs[index])) != empty)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.k);
			h = h * 31 + Integer.hashCode(pair.v);
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
		var mask = kvs.length - 1;
		var index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != empty)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				break;
		var v1 = fun.apply(v0);
		kvs[index] = kv(key, v1);
		size += (v1 != empty ? 1 : 0) - (v0 != empty ? 1 : 0);
		if (v1 == empty)
			new Object() {
				private void rehash(int index) {
					var index1 = (index + 1) & mask;
					var kv_ = kvs[index1];
					var v = v(kv_);
					if (v != empty) {
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
			var source = source_();
			var pair0 = IntIntPair.of(0, 0);
			return IntObjPuller.of(new IntObjSource<Integer>() {
				public boolean source2(IntObjPair<Integer> pair) {
					var b = source.source2(pair0);
					if (b)
						pair.update(pair0.t0, pair0.t1);
					return b;
				}
			});
		});
	}

	public IntStreamlet values() {
		return new IntStreamlet(() -> {
			var source = source_();
			var pair0 = IntIntPair.of(0, 0);
			return IntPuller.of(() -> source.source2(pair0) ? pair0.t1 : empty);
		});
	}

	private void rehash() {
		var capacity = kvs.length;

		if (capacity * 3 / 4 < size) {
			var kvs0 = kvs;
			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++) {
				var kv0 = kvs0[i];
				var v_ = v(kv0);
				if (v_ != empty)
					store(k(kv0), v_);
			}
		}
	}

	private int store(int key, int v1) {
		var capacity = kvs.length;
		var mask = capacity - 1;
		var index = key & mask;
		long kv;
		int v0;
		while ((v0 = v(kv = kvs[index])) != empty)
			if (k(kv) != key)
				index = index + 1 & mask;
			else
				fail("duplicate key " + key);
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
					if ((v = v(kv = kvs[index++])) == empty)
						;
					else {
						pair.update(k(kv), v);
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		kvs = new long[capacity];
		Arrays.fill(kvs, kv(0, empty));
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
