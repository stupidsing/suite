package suite.primitive.adt.map;

import static primal.statics.Fail.fail;

import java.util.Objects;

import primal.fp.Funs.Iterate;
import primal.primitive.FltPrim.FltObjSink;
import primal.primitive.FltPrim.FltObjSource;
import primal.primitive.FltPrim.Flt_Obj;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.puller.FltObjPuller;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.As;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class FltObjMap<V> {

	private static Object EMPTYVALUE = null;

	private int size;
	private float[] ks;
	private Object[] vs;

	public FltObjMap() {
		this(8);
	}

	public FltObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(float key, Flt_Obj<V> fun) {
		var v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltObjMap) {
			var other = (FltObjMap<?>) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.k).equals(pair.v);
			return b;
		} else
			return false;
	}

	public void forEach(FltObjSink<V> sink) {
		var pair = FltObjPair.<V> of((float) 0, null);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.k, pair.v);
	}

	public V get(float key) {
		var index = index(key);
		@SuppressWarnings("unchecked")
		var v = ks[index] == key ? cast(vs[index]) : (V) EMPTYVALUE;
		return v;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.k);
			h = h * 31 + Objects.hashCode(pair.v);
		}
		return h;
	}

	public void put(float key, V v1) {
		size++;
		store(key, v1);
		rehash();
	}

	public void update(float key, Iterate<V> fun) {
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

	public FltObjSource<V> source() {
		return source_();
	}

	public FltObjStreamlet<V> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjPuller.of(source_()));
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

	private void store(float key, Object v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			fail("duplicate key " + key);
	}

	private int index(float key) {
		var mask = vs.length - 1;
		var index = Float.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private FltObjSource<V> source_() {
		return new FltObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltObjPair<V> pair) {
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
		ks = new float[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		var v = (V) o;
		return v;
	}

}
