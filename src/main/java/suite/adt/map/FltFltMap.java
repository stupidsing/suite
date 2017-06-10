package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.FltFltPair;
import suite.primitive.FltFltSink;
import suite.primitive.FltFltSource;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;

/**
 * Map with floatacter key and float value. Float.MIN_VALUE is not allowed in
 * values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltFltMap {

	private int size;
	private float[] ks;
	private float[] vs;

	public FltFltMap() {
		this(8);
	}

	public FltFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(float key, Flt_Flt fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(FltFltSink sink) {
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		FltFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(float key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(float key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public FltFltSource source() {
		return source_();
	}

	// public FltFltStreamlet stream() {
	// return new FltFltStreamlet<>(() -> FltFltOutlet.of(source_()));
	// }

	private float put_(float key, float v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltFltSource source_() {
		return new FltFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltFltPair pair) {
				float v;
				while ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
