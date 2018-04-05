package suite.primitive.adt.map;

import java.util.Objects;

import suite.primitive.ChrPrimitives.ChrObjSink;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

/**
 * Map with primitive integer key and a generic object value. Null values are
 * not allowed. Not thread-safe.
 * 
 * @author ywsing
 */
public class ChrObjMap<V> {

	private int size;
	private char[] ks;
	private Object[] vs;

	public static <V> ChrObjMap<V> collect(ChrObjOutlet<V> outlet) {
		ChrObjMap<V> map = new ChrObjMap<>();
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ChrObjMap() {
		this(8);
	}

	public ChrObjMap(int capacity) {
		allocate(capacity);
	}

	public V computeIfAbsent(char key, Chr_Obj<V> fun) {
		var v = get(key);
		if (v == null)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrObjMap) {
			ChrObjMap<?> other = (ChrObjMap<?>) object;
			var b = size == other.size;
			for (ChrObjPair<V> pair : streamlet())
				b &= other.get(pair.t0).equals(pair.t1);
			return b;
		} else
			return false;
	}

	public void forEach(ChrObjSink<V> sink) {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		ChrObjSource<V> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public V get(char key) {
		var index = index(key);
		return ks[index] == key ? cast(vs[index]) : null;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (ChrObjPair<V> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public void put(char key, V v1) {
		size++;
		store(key, v1);
		rehash();
	}

	public void update(char key, Iterate<V> fun) {
		var mask = vs.length - 1;
		var index = index(key);
		var v0 = cast(vs[index]);
		var v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != null ? 1 : 0) - (v0 != null ? 1 : 0);
		if (v1 == null)
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != null) {
						var k = ks[index1];
						vs[index1] = null;
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

	public ChrObjSource<V> source() {
		return source_();
	}

	public ChrObjStreamlet<V> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(source_()));
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
				if ((o = vs0[i]) != null)
					store(ks0[i], o);
		}
	}

	private void store(char key, Object v1) {
		var index = index(key);
		if (vs[index] == null) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(char key) {
		var mask = vs.length - 1;
		var index = Character.hashCode(key) & mask;
		while (vs[index] != null && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private ChrObjSource<V> source_() {
		return new ChrObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<V> pair) {
				while (index < capacity) {
					var k = ks[index];
					var v = vs[index++];
					if (v != null) {
						pair.update(k, cast(v));
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new char[capacity];
		vs = new Object[capacity];
	}

	private V cast(Object o) {
		@SuppressWarnings("unchecked")
		var v = (V) o;
		return v;
	}

}
