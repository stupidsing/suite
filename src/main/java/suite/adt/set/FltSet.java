package suite.adt.set;

import java.util.Arrays;

import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.Flt_Flt;
import suite.streamlet.FltOutlet;
import suite.streamlet.FltStreamlet;

/**
 * Set with floatacter elements. Float.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class FltSet {

	private int size;
	private float[] vs;

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

	public float put(float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(v_);
			}
		}

		return put_(v);
	}

	public void update(float key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (v != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public FltSource source() {
		return source_();
	}

	public FltStreamlet stream() {
		return new FltStreamlet(() -> FltOutlet.of(source_()));
	}

	private float put_(float v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(v1) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (v0 != v1)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate");
		vs[index] = v1;
		return v0;
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
