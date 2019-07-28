package suite.primitive.adt.map;

import static suite.util.Fail.fail;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrObjSink;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjPuller;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.As;

/**
 * Map with generic object key and character object value. Character.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjChrMap<K> {

	private static char empty = ChrFunUtil.EMPTYVALUE;

	private int size;
	private Object[] ks;
	private char[] vs;

	public static <K> ObjChrMap<K> collect(ChrObjPuller<K> puller) {
		var map = new ObjChrMap<K>();
		var pair = ChrObjPair.<K> of((char) 0, null);
		while (puller.source().source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public ObjChrMap() {
		this(8);
	}

	public ObjChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(K key, Obj_Chr<K> fun) {
		var v = get(key);
		if (v == empty)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjChrMap) {
			@SuppressWarnings("unchecked")
			var other = (ObjChrMap<Object>) object;
			var b = size == other.size;
			for (var pair : streamlet())
				b &= other.get(pair.v) == pair.k;
			return b;
		} else
			return false;
	}

	public void forEach(ChrObjSink<K> sink) {
		var pair = ChrObjPair.<K> of((char) 0, null);
		var source = source_();
		while (source.source2(pair))
			sink.sink2(pair.k, pair.v);
	}

	public char get(K key) {
		var index = index(key);
		return Objects.equals(ks[index], key) ? vs[index] : empty;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.k);
			h = h * 31 + Objects.hashCode(pair.v);
		}
		return h;
	}

	public void put(K key, char v) {
		size++;
		store(key, v);
		rehash();
	}

	public void update(K key, Chr_Chr fun) {
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

	public ChrObjSource<K> source() {
		return source_();
	}

	public ChrObjStreamlet<K> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(source_()));
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
			char v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					store(ks0[i], v_);
		}
	}

	private void store(Object key, char v1) {
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

	private ChrObjSource<K> source_() {
		return new ChrObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<K> pair) {
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
		vs = new char[capacity];
		Arrays.fill(vs, empty);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		var k = (K) o;
		return k;
	}

}
