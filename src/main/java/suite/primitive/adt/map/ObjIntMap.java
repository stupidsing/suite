package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import primal.Verbs.Equals;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntObjSink;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.Int_Int;
import primal.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjPuller;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;

/**
 * Map with generic object key and intacter object value. Integer.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjIntMap<K> {

	private static int empty = IntPrim.EMPTYVALUE;

	private int size;
	private Object[] ks;
	private int[] vs;

	public static <K> ObjIntMap<K> collect(IntObjPuller<K> puller) {
		var map = new ObjIntMap<K>();
		var pair = IntObjPair.<K> of((int) 0, null);
		while (puller.source().source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjIntMap) {
			@SuppressWarnings("unchecked")
			var other = (ObjIntMap<Object>) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.v) == pair.k;
			return b;
		} else
			return false;
	}

	public void forEach(IntObjSink<K> sink) {
		var pair = IntObjPair.<K> of((int) 0, null);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.k, pair.v);
	}

	public int get(K key) {
		var index = index(key);
		return Equals.ab(ks[index], key) ? vs[index] : empty;
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

	public void put(K key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	public void update(K key, Int_Int fun) {
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
					var v = vs[index1];
					if (v != empty) {
						var k = ks[index1];
						vs[index1] = empty;
						rehash(index1);
						store(k, v);
					}
				}
			}.rehash(index);

		rehash();
	}

	public int size() {
		return size;
	}

	public IntObjSource<K> source() {
		return source_();
	}

	public IntObjStreamlet<K> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
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

	private void store(Object key, int v1) {
		var index = index(key);
		if (vs[index] == empty) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(Object key) {
		var mask = vs.length - 1;
		var index = key.hashCode() & mask;
		while (vs[index] != empty && !ks[index].equals(key))
			index = index + 1 & mask;
		return index;
	}

	private IntObjSource<K> source_() {
		return new IntObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntObjPair<K> pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != empty) {
						pair.update(v, cast(k));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, empty);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		var k = (K) o;
		return k;
	}

}
