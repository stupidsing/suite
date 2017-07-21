package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrObjSink;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.util.FunUtil.Fun;

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

	public static <K> Fun<ChrObjOutlet<K>, ObjChrMap<K>> collect() {
		return outlet -> {
			ObjChrMap<K> map = new ObjChrMap<>();
			ChrObjPair<K> pair = ChrObjPair.of((char) 0, null);
			while (outlet.source().source2(pair))
				map.put(pair.t1, pair.t0);
			return map;
		};
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

	public char put(K key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public ChrObjSource<K> source() {
		return source_();
	}

	public ChrObjStreamlet<K> stream() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(source_()));
	}

	private char put_(Object key, char v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrObjSource<K> source_() {
		return new ChrObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrObjPair<K> pair) {
				char v;
				while ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t1 = cast(ks[index++]);
				pair.t0 = v;
				return true;
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
