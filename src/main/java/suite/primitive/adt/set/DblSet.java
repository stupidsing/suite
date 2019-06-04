package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.Doubles_;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.streamlet.DblPuller;
import suite.primitive.streamlet.DblStreamlet;

/**
 * Set with doubleacter elements. Double.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class DblSet {

	private static double empty = DblFunUtil.EMPTYVALUE;

	private int size;
	private double[] vs;

	public static DblSet intersect(DblSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static DblSet union(DblSet... sets) {
		var set = new DblSet();
		for (var set_ : sets)
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

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			double v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					add_(v_);
		}

		var b = add_(c);
		size += b ? 1 : 0;
		return b;
	}

	public boolean contains(double c) {
		return vs[index(c)] == c;
	}

	public DblSet clone() {
		var capacity = vs.length;
		var set = new DblSet(capacity);
		set.size = size;
		Doubles_.copy(vs, 0, set.vs, 0, capacity);
		return set;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblSet) {
			var other = (DblSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(DblSink sink) {
		var source = source_();
		double c;
		while ((c = source.g()) != empty)
			sink.f(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Double.hashCode(c);
		return h;
	}

	public boolean remove(double c) {
		var mask = vs.length - 1;
		var index = index(c);
		var b = vs[index] == c;
		if (b) {
			vs[index] = empty;
			size--;
			new Object() {
				private void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != empty) {
						vs[index1] = empty;
						rehash(index1);
						vs[index(v)] = v;
					}
				}
			}.rehash(index);
		}
		return b;
	}

	public int size() {
		return size;
	}

	public DblSource source() {
		return source_();
	}

	public DblStreamlet streamlet() {
		return new DblStreamlet(() -> DblPuller.of(source_()));
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(double c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(double c) {
		var mask = vs.length - 1;
		var index = Double.hashCode(c) & mask;
		double c0;
		while ((c0 = vs[index]) != empty && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private DblSource source_() {
		return new DblSource() {
			private int capacity = vs.length;
			private int index = 0;

			public double g() {
				double v;
				while (index < capacity)
					if ((v = vs[index++]) != empty)
						return v;
				return empty;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new double[capacity];
		Arrays.fill(vs, empty);
	}

}
