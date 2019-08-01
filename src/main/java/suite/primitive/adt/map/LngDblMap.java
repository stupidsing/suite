package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import primal.fp.Funs.Fun;
import primal.primitive.DblPrim;
import primal.primitive.LngDblSink;
import primal.primitive.LngDblSource;
import primal.primitive.LngPrim.LngObjSource;
import primal.primitive.adt.pair.LngDblPair;
import primal.primitive.adt.pair.LngObjPair;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Dbl;
import suite.primitive.streamlet.LngObjPuller;
import suite.primitive.streamlet.LngObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Puller;

/**
 * Map with primitive long key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class LngDblMap {

	private static double empty = DblPrim.EMPTYVALUE;

	private int size;
	private long[] ks;
	private double[] vs;

	public static <T> Fun<Puller<T>, LngDblMap> collect(Obj_Lng<T> kf0, Obj_Dbl<T> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return puller -> {
			var map = new LngDblMap();
			T t;
			while ((t = puller.source().g()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public LngDblMap() {
		this(8);
	}

	public LngDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(long key, Lng_Dbl fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngDblMap) {
			var other = (LngDblMap) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k) == pair.v;
			return b;
		} else
			return false;
	}

	public void forEach(LngDblSink sink) {
		var pair = LngDblPair.of((long) 0, (double) 0);
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

	public double get(long key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : empty;
	}

	public void put(long key, double v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(long key, Dbl_Dbl fun) {
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

	public LngDblSource source() {
		return source_();
	}

	public LngObjStreamlet<Double> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjPuller.of(new LngObjSource<Double>() {
			private LngDblSource source0 = source_();
			private LngDblPair pair0 = LngDblPair.of((long) 0, (double) 0);

			public boolean source2(LngObjPair<Double> pair) {
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
			double v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					store(ks0[i], v_);
		}
	}

	private void store(long key, double v1) {
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

	private LngDblSource source_() {
		return new LngDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngDblPair pair) {
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
		vs = new double[capacity];
		Arrays.fill(vs, empty);
	}

}
