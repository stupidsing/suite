package suite.primitive.adt.map;

import static suite.util.Friends.fail;

import java.util.Objects;

import suite.primitive.IntPrimitives.IntObjSink;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntObjMap<V> {

	private static Object EMPTYVALUE = null;

	private int size;
	private int[] ks;
	private Object[] vs;

	public static <V> IntObjMap<V> collect(IntObjOutlet<V> outlet) {
		var map = new IntObjMap<V>();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public IntObjMap() {
		this(8);
	}

	public IntObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(int key, Int_Obj<V> fun) {
		var v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntObjMap) {
			var other = (IntObjMap<?>) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(IntObjSink<V> sink) {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(int key) {
		var index = index(key);
		@SuppressWarnings("unchecked")
		V v = ks[index] == key ? cast(vs[index]) : (V) EMPTYVALUE;
		return v;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public void put(int key, V v1) {
		size++;
		store(key, v1);
		rehash();
	}

	public void update(int key, Iterate<V> fun) {
		var mask = vs.length - 1;
		var index = index(key);
		var v0 = cast(vs[index]);
		var v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				private void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != EMPTYVALUE) {
						var k = ks[index1];
						vs[index1] = EMPTYVALUE;
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

	public IntObjSource<V> source() {
		return source_();
	}

	public IntObjStreamlet<V> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var ks0 = ks;
			var vs0 = vs;
			Object o;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((o = vs0[i]) != EMPTYVALUE)
					store(ks0[i], o);
		}
	}

	private void store(int key, Object v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(int key) {
		var mask = vs.length - 1;
		var index = Integer.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private IntObjSource<V> source_() {
		return new IntObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntObjPair<V> pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != EMPTYVALUE) {
						pair.update(k, cast(v));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		var v = (V) o;
		return v;
	}

}
