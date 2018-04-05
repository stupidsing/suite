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

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private int size;
	private double[] vs;

	public static DblSet intersect(DblSet... sets) {
		return sets[0].streamlet().filter(c -> {
			boolean b = true;
			for (DblSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static DblSet union(DblSet... sets) {
		DblSet set = new DblSet();
		for (DblSet set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public DblSet() {
		this(8);
	}

	public DblSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(double c) {
		var capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			double[] vs0 = vs;
			double v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(double c) {
		return vs[index(c)] == c;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblSet) {
			DblSet other = (DblSet) object;
			boolean b = size == other.size;
			for (double c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(DblSink sink) {
		DblSource source = source_();
		double c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (double c : streamlet())
			h = h * 31 + Double.hashCode(c);
		return h;
	}

	public DblSource source() {
		return source_();
	}

	public DblStreamlet streamlet() {
		return new DblStreamlet(() -> DblOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (double c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(double c) {
		var index = index(c);
		boolean b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(double c) {
		var mask = vs.length - 1;
		var index = Double.hashCode(c) & mask;
		double c0;
		while ((c0 = vs[index]) != EMPTYVALUE && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private DblSource source_() {
		return new DblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public double source() {
				double v;
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new double[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
