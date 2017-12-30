package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngObjSink;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.streamlet.LngObjOutlet;
import suite.primitive.streamlet.LngObjStreamlet;

/**
 * Map with generic object key and longacter object value. Long.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjLngMap<K> {

	private int size;
	private Object[] ks;
	private long[] vs;

	public static <K> ObjLngMap<K> collect(LngObjOutlet<K> outlet) {
		ObjLngMap<K> map = new ObjLngMap<>();
		LngObjPair<K> pair = LngObjPair.of((long) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public ObjLngMap() {
		this(8);
	}

	public ObjLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(K key, Obj_Lng<K> fun) {
		long v = get(key);
		if (v == LngFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjLngMap) {
			@SuppressWarnings("unchecked")
			ObjLngMap<Object> other = (ObjLngMap<Object>) object;
			boolean b = size == other.size;
			for (LngObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(LngObjSink<K> sink) {
		LngObjPair<K> pair = LngObjPair.of((long) 0, null);
		LngObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (LngObjPair<K> pair : streamlet()) {
			h = h * 31 + Long.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public long get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(K key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v;
		while ((v = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		size += ((vs[index] = fun.apply(v)) != LngFunUtil.EMPTYVALUE ? 1 : 0) - (v != LngFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public LngObjSource<K> source() {
		return source_();
	}

	public LngObjStreamlet<K> streamlet() {
		return new LngObjStreamlet<>(() -> LngObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LngObjPair<K> pair : streamlet())
			sb.append(pair.t1 + ":" + pair.t0 + ",");
		return sb.toString();
	}

	private long put_(Object key, long v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		long v0;
		while ((v0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private LngObjSource<K> source_() {
		return new LngObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(LngObjPair<K> pair) {
				long v;
				while (index < capacity)
					if ((v = vs[index]) == LngFunUtil.EMPTYVALUE)
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
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
