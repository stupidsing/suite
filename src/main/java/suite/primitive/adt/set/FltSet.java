package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.Floats_;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.streamlet.FltPuller;
import suite.primitive.streamlet.FltStreamlet;

/**
 * Set with floatacter elements. Float.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class FltSet {

	private static float empty = FltFunUtil.EMPTYVALUE;

	private int size;
	private float[] vs;

	public static FltSet intersect(FltSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static FltSet union(FltSet... sets) {
		var set = new FltSet();
		for (var set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public FltSet() {
		this(8);
	}

	public FltSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(float c) {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			float v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					add_(v_);
		}

		var b = add_(c);
		size += b ? 1 : 0;
		return b;
	}

	public boolean contains(float c) {
		return vs[index(c)] == c;
	}

	public FltSet clone() {
		var capacity = vs.length;
		var set = new FltSet(capacity);
		set.size = size;
		Floats_.copy(vs, 0, set.vs, 0, capacity);
		return set;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltSet) {
			var other = (FltSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(FltSink sink) {
		var source = source_();
		float c;
		while ((c = source.g()) != empty)
			sink.f(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Float.hashCode(c);
		return h;
	}

	public boolean remove(float c) {
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

	public FltSource source() {
		return source_();
	}

	public FltStreamlet streamlet() {
		return new FltStreamlet(() -> FltPuller.of(source_()));
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(float c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(float c) {
		var mask = vs.length - 1;
		var index = Float.hashCode(c) & mask;
		float c0;
		while ((c0 = vs[index]) != empty && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private FltSource source_() {
		return new FltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public float g() {
				float v;
				while (index < capacity)
					if ((v = vs[index++]) != empty)
						return v;
				return empty;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new float[capacity];
		Arrays.fill(vs, empty);
	}

}
