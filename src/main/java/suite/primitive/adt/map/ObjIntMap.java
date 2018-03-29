package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntObjSink;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;

/**
 * Map with generic object key and intacter object value. Integer.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjIntMap<K> {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private Object[] ks;
	private int[] vs;

	public static <K> ObjIntMap<K> collect(IntObjOutlet<K> outlet) {
		ObjIntMap<K> map = new ObjIntMap<>();
		IntObjPair<K> pair = IntObjPair.of((int) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		int v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjIntMap) {
			@SuppressWarnings("unchecked")
			ObjIntMap<Object> other = (ObjIntMap<Object>) object;
			boolean b = size == other.size;
			for (IntObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(IntObjSink<K> sink) {
		IntObjPair<K> pair = IntObjPair.of((int) 0, null);
		IntObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<K> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public int put(K key, int v) {
		size++;
		int v0 = store(key, v);
		rehash();
		return v0;
	}

	public void update(K key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		int v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					int v = vs[index1];
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

	public IntObjSource<K> source() {
		return source_();
	}

	public IntObjStreamlet<K> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private int store(Object key, int v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				Fail.t("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntObjSource<K> source_() {
		return new IntObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntObjPair<K> pair) {
				int v;
				while (index < capacity)
					if ((v = vs[index]) == EMPTYVALUE)
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
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
