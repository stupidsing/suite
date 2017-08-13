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

	public void forEach(FltSink sink) {
		FltSource source = source_();
		float c;
		while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	public boolean add(float c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(c);
	}

	public boolean contains(float c) {
		return 0 <= index(c);
	}

	public FltSource source() {
		return source_();
	}

	public FltStreamlet streamlet() {
		return new FltStreamlet(() -> FltOutlet.of(source_()));
	}

	private boolean add_(float c) {
		int index = index(c);
		if (0 <= index) {
			vs[index] = c;
			return true;
		} else
			return false;
	}

	private int index(float c) {
		int mask = vs.length - 1;
		int index = Float.hashCode(c) & mask;
		float c0;
		while ((c0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (c0 != c)
				index = index + 1 & mask;
			else
				return -1;
		return index;
	}

	private FltSource source_() {
		return new FltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public float source() {
				float v;
				while ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return FltFunUtil.EMPTYVALUE;
				return v;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
