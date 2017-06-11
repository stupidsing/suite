package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.DblFltPair;
import suite.primitive.DblFltSink;
import suite.primitive.DblFltSource;
import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Flt;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;

/**
 * Map with primitive double key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblFltMap {

	private int size;
	private double[] ks;
	private float[] vs;

	public DblFltMap() {
		this(8);
	}

	public DblFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(double key, Dbl_Flt fun) {
		float v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblFltSink sink) {
		DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
		DblFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(double key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(double key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public DblFltSource source() {
		return source_();
	}

	// public DblFltStreamlet stream() {
	// return new DblFltStreamlet<>(() -> DblFltOutlet.of(source_()));
	// }

	private float put_(double key, float v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblFltSource source_() {
		return new DblFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblFltPair pair) {
				float v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
