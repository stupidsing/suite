package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrObjSink;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;

/**
 * Map with generic object key and character object value. Character.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjChrMap<K> {

	private static char EMPTYVALUE = ChrFunUtil.EMPTYVALUE;

	private int size;
	private Object[] ks;
	private char[] vs;

	public static <K> ObjChrMap<K> collect(ChrObjOutlet<K> outlet) {
		ObjChrMap<K> map = new ObjChrMap<>();
		ChrObjPair<K> pair = ChrObjPair.of((char) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
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
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjChrMap) {
			@SuppressWarnings("unchecked")
			ObjChrMap<Object> other = (ObjChrMap<Object>) object;
			boolean b = size == other.size;
			for (ChrObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(ChrObjSink<K> sink) {
		ChrObjPair<K> pair = ChrObjPair.of((char) 0, null);
		ChrObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(K key) {
		var index = index(key);
		return Objects.equals(ks[index], key) ? vs[index] : EMPTYVALUE;
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (ChrObjPair<K> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
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
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != EMPTYVALUE) {
						Object k = ks[index1];
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

	public ChrObjSource<K> source() {
		return source_();
	}

	public ChrObjStreamlet<K> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			char[] vs0 = vs;
			char v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(Object key, char v1) {
		var index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(Object key) {
		var mask = vs.length - 1;
		var index = key.hashCode() & mask;
		while (vs[index] != EMPTYVALUE && !ks[index].equals(key))
			index = index + 1 & mask;
		return index;
	}

	private ChrObjSource<K> source_() {
		return new ChrObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<K> pair) {
				while (index < capacity) {
					Object k = ks[index];
					var v = vs[index++];
					if (v != EMPTYVALUE) {
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
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
