package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;

/**
 * Set with floatacter elements. Float.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class FltSet {

	private static float EMPTYVALUE = FltFunUtil.EMPTYVALUE;

	private int size;
	private float[] vs;

	public static FltSet intersect(FltSet... sets) {
		return sets[0].streamlet().filter(c -> {
			boolean b = true;
			for (FltSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static FltSet union(FltSet... sets) {
		FltSet set = new FltSet();
		for (FltSet set_ : sets)
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
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			float[] vs0 = vs;
			float v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(float c) {
		return vs[index(c)] == c;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltSet) {
			FltSet other = (FltSet) object;
			boolean b = size == other.size;
			for (float c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(FltSink sink) {
		FltSource source = source_();
		float c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (float c : streamlet())
			h = h * 31 + Float.hashCode(c);
		return h;
	}

	public FltSource source() {
		return source_();
	}

	public FltStreamlet streamlet() {
		return new FltStreamlet(() -> FltOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (float c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(float c) {
		int index = index(c);
		boolean b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(float c) {
		int mask = vs.length - 1;
		int index = Float.hashCode(c) & mask;
		float c0;
		while ((c0 = vs[index]) != EMPTYVALUE && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private FltSource source_() {
		return new FltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public float source() {
				float v;
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new float[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
