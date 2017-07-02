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

	public static ChrSet intersect(ChrSet... sets) {
		return sets[0].stream().filter(c -> {
			boolean b = true;
			for (ChrSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static ChrSet union(ChrSet... sets) {
		ChrSet set = new ChrSet();
		for (ChrSet set_ : sets)
			set_.stream().sink(set::add);
		return set;
	}

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

	public boolean add(char c) {
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

		return add_(c);
	}

	public boolean contains(char c) {
		return 0 <= index(c);
	}

	public ChrSource source() {
		return source_();
	}

	public ChrStreamlet stream() {
		return new ChrStreamlet(() -> ChrOutlet.of(source_()));
	}

	private boolean add_(char c) {
		int index = index(c);
		if (0 <= index) {
			vs[index] = c;
			return true;
		} else
			return false;
	}

	private int index(char c) {
		int mask = vs.length - 1;
		int index = Character.hashCode(c) & mask;
		char c0;
		while ((c0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (c0 != c)
				index = index + 1 & mask;
			else
				return -1;
		return index;
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
