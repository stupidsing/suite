package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import primal.fp.Funs.Fun;
import primal.primitive.IntPrim;
import primal.primitive.LngIntSink;
import primal.primitive.LngIntSource;
import primal.primitive.LngPrim.LngObjSource;
import primal.primitive.adt.pair.LngIntPair;
import primal.primitive.adt.pair.LngObjPair;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Int;
import suite.primitive.streamlet.LngObjPuller;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Puller;

/**
 * Map with primitive long key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngIntMap {

	private static int empty = IntPrim.EMPTYVALUE;

	private int size;
	private long[] ks;
	private int[] vs;

	public static <T> Fun<Puller<T>, LngIntMap> collect(Obj_Lng<T> kf0, Obj_Int<T> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return puller -> {
			var map = new LngIntMap();
			T t;
			while ((t = puller.source().g()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngIntMap() {
		this(8);
	}

	public LngIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(long key, Lng_Int fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngIntMap) {
			var other = (LngIntMap) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k) == pair.v;
			return b;
		} else
			return false;
	}

	public void forEach(LngIntSink sink) {
		var pair = LngIntPair.of((long) 0, (int) 0);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.k);
			h = h * 31 + Objects.hashCode(pair.v);
		}
		return h;
	}

	public int get(long key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : empty;
	}

	public void put(long key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Int_Int fun) {
		var mask = vs.length - 1;
		var index = index(key);
		var v0 = vs[index];
		var v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != empty ? 1 : 0) - (v0 != empty ? 1 : 0);
		if (v1 == empty)
			new Object() {
				private void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v_ = vs[index1];
					if (v_ != empty) {
						var k = ks[index1];
						vs[index1] = empty;
						rehash(index1);
						store(k, v_);
					}
				}
			}.rehash(index);
		rehash();
	}

	public int size() {
		return size;
	}

	public LngIntSource source() {
		return source_();
	}

	public LngObjStreamlet<Integer> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(new LngObjSource<Integer>() {
			private LngIntSource source0 = source_();
			private LngIntPair pair0 = LngIntPair.of((long) 0, (int) 0);

			public boolean source2(LngObjPair<Integer> pair) {
				var b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var ks0 = ks;
			var vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					store(ks0[i], v_);
		}
	}

	private void store(long key, int v1) {
		var index = index(key);
		if (vs[index] == empty) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(long key) {
		var mask = vs.length - 1;
		var index = Long.hashCode(key) & mask;
		while (vs[index] != empty && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private LngIntSource source_() {
		return new LngIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngIntPair pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != empty) {
						pair.update(k, v);
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new long[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, empty);
	}

}
