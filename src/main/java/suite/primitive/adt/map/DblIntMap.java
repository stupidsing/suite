package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import primal.fp.Funs.Fun;
import primal.primitive.DblIntSink;
import primal.primitive.DblIntSource;
import primal.primitive.DblPrim.DblObjSource;
import primal.primitive.IntPrim;
import primal.primitive.adt.pair.DblIntPair;
import primal.primitive.adt.pair.DblObjPair;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Int;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.streamlet.DblObjPuller;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Puller;

/**
 * Map with primitive double key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblIntMap {

	private static int empty = IntPrim.EMPTYVALUE;

	private int size;
	private double[] ks;
	private int[] vs;

	public static <T> Fun<Puller<T>, DblIntMap> collect(Obj_Dbl<T> kf0, Obj_Int<T> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return puller -> {
			var map = new DblIntMap();
			T t;
			while ((t = puller.source().g()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblIntMap() {
		this(8);
	}

	public DblIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(double key, Dbl_Int fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblIntMap) {
			var other = (DblIntMap) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k) == pair.v;
			return b;
		} else
			return false;
	}

	public void forEach(DblIntSink sink) {
		var pair = DblIntPair.of((double) 0, (int) 0);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.k);
			h = h * 31 + Objects.hashCode(pair.v);
		}
		return h;
	}

	public int get(double key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : empty;
	}

	public void put(double key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(double key, Int_Int fun) {
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

	public DblIntSource source() {
		return source_();
	}

	public DblObjStreamlet<Integer> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(new DblObjSource<Integer>() {
			private DblIntSource source0 = source_();
			private DblIntPair pair0 = DblIntPair.of((double) 0, (int) 0);

			public boolean source2(DblObjPair<Integer> pair) {
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

	private void store(double key, int v1) {
		var index = index(key);
		if (vs[index] == empty) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(double key) {
		var mask = vs.length - 1;
		var index = Double.hashCode(key) & mask;
		while (vs[index] != empty && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private DblIntSource source_() {
		return new DblIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblIntPair pair) {
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
		ks = new double[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, empty);
	}

}
