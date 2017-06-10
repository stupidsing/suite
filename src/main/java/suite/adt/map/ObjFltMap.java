package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.FltObjPair;
import suite.primitive.FltPrimitives.FltObjSink;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.streamlet.FltObjOutlet;
import suite.streamlet.FltObjStreamlet;

/**
 * Map with generic object key and floatacter object value. Float.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjFltMap<K> {

	public final static float EMPTYVALUE = Float.MIN_VALUE;

	private int size;
	private Object[] ks;
	private float[] vs;

	public ObjFltMap() {
		this(8);
	}

	public ObjFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(K key, Obj_Flt<K> fun) {
		float v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
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
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(K key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		float v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public FltObjSource<K> source() {
		return source_();
	}

	public FltObjStreamlet<K> stream() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(source_()));
	}

	private float put_(Object key, float v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		float v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltObjSource<K> source_() {
		return new FltObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltObjPair<K> pair) {
				float v;
				while ((v = vs[index]) == EMPTYVALUE)
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
		vs = new float[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
