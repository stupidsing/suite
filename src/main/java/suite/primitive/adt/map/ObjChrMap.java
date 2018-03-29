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
		char v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
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
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (ChrObjPair<K> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public char put(K key, char v) {
		size++;
		char v0 = store(key, v);
		rehash();
		return v0;
	}

	public void update(K key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		char v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != ChrFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != ChrFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == ChrFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					char v = vs[index1];
					if (v != ChrFunUtil.EMPTYVALUE) {
						Object k = ks[index1];
						vs[index1] = ChrFunUtil.EMPTYVALUE;
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
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private char store(Object key, char v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				Fail.t("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrObjSource<K> source_() {
		return new ChrObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<K> pair) {
				char v;
				while (index < capacity)
					if ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(v, cast(ks[index++]));
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
