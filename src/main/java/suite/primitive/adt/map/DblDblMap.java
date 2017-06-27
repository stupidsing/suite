package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.DblDblSink;
import suite.primitive.DblDblSource;
import suite.primitive.DblFunUtil;
import suite.primitive.Dbl_Dbl;
import suite.primitive.adt.pair.DblDblPair;

/**
 * Map with primitive double key and primitive double value. Double.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblDblMap {

	private int size;
	private double[] ks;
	private double[] vs;

	public DblDblMap() {
		this(8);
	}

	public DblDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(double key, Dbl_Dbl fun) {
		double v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblDblSink sink) {
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		DblDblSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public double get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public double put(double key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(double key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public DblDblSource source() {
		return source_();
	}

	// public DblDblStreamlet stream() {
	// return new DblDblStreamlet<>(() -> DblDblOutlet.of(source_()));
	// }

	private double put_(double key, double v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblDblSource source_() {
		return new DblDblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblDblPair pair) {
				double v;
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
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
