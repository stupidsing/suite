package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import primal.fp.Funs.Fun;
import primal.primitive.FltPrim;
import primal.primitive.Flt_Flt;
import primal.primitive.IntFltSink;
import primal.primitive.IntFltSource;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.Int_Flt;
import primal.primitive.adt.pair.IntFltPair;
import primal.primitive.adt.pair.IntObjPair;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.streamlet.IntObjPuller;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Puller;

/**
 * Map with primitive int key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntFltMap {

	private static float empty = FltPrim.EMPTYVALUE;

	private int size;
	private int[] ks;
	private float[] vs;

	public static <T> Fun<Puller<T>, IntFltMap> collect(Obj_Int<T> kf0, Obj_Flt<T> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return puller -> {
			var map = new IntFltMap();
			T t;
			while ((t = puller.source().g()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntFltMap() {
		this(8);
	}

	public IntFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(int key, Int_Flt fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntFltMap) {
			var other = (IntFltMap) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k) == pair.v;
			return b;
		} else
			return false;
	}

	public void forEach(IntFltSink sink) {
		var pair = IntFltPair.of((int) 0, (float) 0);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.k);
			h = h * 31 + Objects.hashCode(pair.v);
		}
		return h;
	}

	public float get(int key) {
		var index = index(key);
		return ks[index] == key ? vs[index] : empty;
	}

	public void put(int key, float v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Flt_Flt fun) {
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

	public IntFltSource source() {
		return source_();
	}

	public IntObjStreamlet<Float> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(new IntObjSource<Float>() {
			private IntFltSource source0 = source_();
			private IntFltPair pair0 = IntFltPair.of((int) 0, (float) 0);

			public boolean source2(IntObjPair<Float> pair) {
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
			float v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					store(ks0[i], v_);
		}
	}

	private void store(int key, float v1) {
		var index = index(key);
		if (vs[index] == empty) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(int key) {
		var mask = vs.length - 1;
		var index = Integer.hashCode(key) & mask;
		while (vs[index] != empty && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private IntFltSource source_() {
		return new IntFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntFltPair pair) {
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
		ks = new int[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, empty);
	}

}
