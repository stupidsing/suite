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

	private static char EMPTYVALUE = ChrFunUtil.EMPTYVALUE;

	private int size;
	private char[] vs;

	public static ChrSet intersect(ChrSet... sets) {
		return sets[0].streamlet().filter(c -> {
			boolean b = true;
			for (ChrSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static ChrSet union(ChrSet... sets) {
		ChrSet set = new ChrSet();
		for (ChrSet set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public ChrSet() {
		this(8);
	}

	public ChrSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(char c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			char[] vs0 = vs;
			char v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					add_(v_);
		}

		return add_(c);
	}

	public boolean contains(char c) {
		return 0 <= index(c);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrSet) {
			ChrSet other = (ChrSet) object;
			boolean b = size == other.size;
			for (char c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(ChrSink sink) {
		ChrSource source = source_();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (char c : streamlet())
			h = h * 31 + Character.hashCode(c);
		return h;
	}

	public ChrSource source() {
		return source_();
	}

	public ChrStreamlet streamlet() {
		return new ChrStreamlet(() -> ChrOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (char c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(char c) {
		int index = index(c);
		boolean b = 0 <= index;
		if (b)
			vs[index] = c;
		return b;
	}

	private int index(char c) {
		int mask = vs.length - 1;
		int index = Character.hashCode(c) & mask;
		char c0;
		while ((c0 = vs[index]) != EMPTYVALUE)
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
				while (index < capacity)
					if ((v = vs[index++]) != EMPTYVALUE)
						return v;
				return EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new char[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
