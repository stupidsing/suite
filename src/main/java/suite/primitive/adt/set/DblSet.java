package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.DblStreamlet;

/**
 * Set with doubleacter elements. Double.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class DblSet {

	private int size;
	private double[] vs;

	public static DblSet intersect(DblSet... sets) {
		return sets[0].stream().filter(c -> {
			boolean b = true;
			for (DblSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static DblSet union(DblSet... sets) {
		DblSet set = new DblSet();
		for (DblSet set_ : sets)
			set_.stream().sink(set::add);
		return set;
	}

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

	public boolean add(double c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(c);
	}

	public boolean contains(double c) {
		return 0 <= index(c);
	}

	public DblSource source() {
		return source_();
	}

	public DblStreamlet stream() {
		return new DblStreamlet(() -> DblOutlet.of(source_()));
	}

	private boolean add_(double c) {
		int index = index(c);
		if (0 <= index) {
			vs[index] = c;
			return true;
		} else
			return false;
	}

	private int index(double c) {
		int mask = vs.length - 1;
		int index = Double.hashCode(c) & mask;
		double c0;
		while ((c0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (c0 != c)
				index = index + 1 & mask;
			else
				return -1;
		return index;
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
