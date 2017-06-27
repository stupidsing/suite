package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrFltSink;
import suite.primitive.ChrFltSource;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Flt;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.pair.ChrFltPair;

/**
 * Map with primitive char key and primitive float value. Float.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrFltMap {

	private int size;
	private char[] ks;
	private float[] vs;

	public ChrFltMap() {
		this(8);
	}

	public ChrFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(char key, Chr_Flt fun) {
		float v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrFltSink sink) {
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		ChrFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public float get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(char key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public ChrFltSource source() {
		return source_();
	}

	// public ChrFltStreamlet stream() {
	// return new ChrFltStreamlet<>(() -> ChrFltOutlet.of(source_()));
	// }

	private float put_(char key, float v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrFltSource source_() {
		return new ChrFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrFltPair pair) {
				float v;
				while ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new char[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
