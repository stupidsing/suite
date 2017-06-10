package suite.adt.set;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.Dbl_Dbl;
import suite.streamlet.DblOutlet;
import suite.streamlet.DblStreamlet;

/**
 * Set with doubleacter elements. Double.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class DblSet {

	private int size;
	private double[] vs;

	public DblSet() {
		this(8);
	}

	public DblSet(int capacity) {
		allocate(capacity);
	}

	public void forEach(DblSink sink) {
		DblSource source = source_();
		double c;
		while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	public double put(double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(v_);
			}
		}

		return put_(v);
	}

	public void update(double key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (v != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public DblSource source() {
		return source_();
	}

	public DblStreamlet stream() {
		return new DblStreamlet(() -> DblOutlet.of(source_()));
	}

	private double put_(double v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(v1) & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (v0 != v1)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate");
		vs[index] = v1;
		return v0;
	}

	private DblSource source_() {
		return new DblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public double source() {
				double v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return DblFunUtil.EMPTYVALUE;
				return v;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

}
