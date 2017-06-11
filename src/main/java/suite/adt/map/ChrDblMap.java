package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.ChrDblPair;
import suite.primitive.ChrDblSink;
import suite.primitive.ChrDblSource;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Dbl;
import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Dbl;

/**
 * Map with primitive char key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrDblMap {

	private int size;
	private char[] ks;
	private double[] vs;

	public ChrDblMap() {
		this(8);
	}

	public ChrDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(char key, Chr_Dbl fun) {
		double v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrDblSink sink) {
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		ChrDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public double get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(char key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public ChrDblSource source() {
		return source_();
	}

	// public ChrDblStreamlet stream() {
	// return new ChrDblStreamlet<>(() -> ChrDblOutlet.of(source_()));
	// }

	private double put_(char key, double v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrDblSource source_() {
		return new ChrDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrDblPair pair) {
				double v;
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
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
