package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.ChrStreamlet;

/**
 * Set with character elements. Character.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class ChrSet {

	private int size;
	private char[] vs;

	public ChrSet() {
		this(8);
	}

	public ChrSet(int capacity) {
		allocate(capacity);
	}

	public void forEach(ChrSink sink) {
		ChrSource source = source_();
		char c;
		while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	public boolean add(char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(v);
	}

	public ChrSource source() {
		return source_();
	}

	public ChrStreamlet stream() {
		return new ChrStreamlet(() -> ChrOutlet.of(source_()));
	}

	private boolean add_(char v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(v1) & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (v0 != v1)
				index = index + 1 & mask;
			else
				return false;
		vs[index] = v1;
		return true;
	}

	private ChrSource source_() {
		return new ChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public char source() {
				char v;
				while ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return ChrFunUtil.EMPTYVALUE;
				return v;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
