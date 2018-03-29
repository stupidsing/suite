package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltObjSink;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;

/**
 * Map with generic object key and floatacter object value. Float.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjFltMap<K> {

	private int size;
	private Object[] ks;
	private float[] vs;

	public static <K> ObjFltMap<K> collect(FltObjOutlet<K> outlet) {
		ObjFltMap<K> map = new ObjFltMap<>();
		FltObjPair<K> pair = FltObjPair.of((float) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public ObjFltMap() {
		this(8);
	}

	public ObjFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(K key, Obj_Flt<K> fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjFltMap) {
			@SuppressWarnings("unchecked")
			ObjFltMap<Object> other = (ObjFltMap<Object>) object;
			boolean b = size == other.size;
			for (FltObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(FltObjSink<K> sink) {
		FltObjPair<K> pair = FltObjPair.of((float) 0, null);
		FltObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (FltObjPair<K> pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public float put(K key, float v) {
		size++;
		float v0 = store(key, v);
		rehash();
		return v0;
	}

	public void update(K key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		float v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != FltFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == FltFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					float v = vs[index1];
					if (v != FltFunUtil.EMPTYVALUE) {
						Object k = ks[index1];
						vs[index1] = FltFunUtil.EMPTYVALUE;
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

	public FltObjSource<K> source() {
		return source_();
	}

	public FltObjStreamlet<K> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private float store(Object key, float v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				Fail.t("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltObjSource<K> source_() {
		return new FltObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltObjPair<K> pair) {
				float v;
				while (index < capacity)
					if ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
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
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
