package suite.primitive.adt.set;

import java.util.Arrays;

import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;

/**
 * Set with longacter elements. Long.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class LngSet {

	private int size;
	private long[] vs;

	public static LngSet intersect(LngSet... sets) {
		return sets[0].streamlet().filter(c -> {
			boolean b = true;
			for (LngSet set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static LngSet union(LngSet... sets) {
		LngSet set = new LngSet();
		for (LngSet set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public LngSet() {
		this(8);
	}

	public LngSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(long c) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			long[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != LngFunUtil.EMPTYVALUE)
					add_(v_);
			}
		}

		return add_(c);
	}

	public boolean contains(long c) {
		return 0 <= index(c);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngSet) {
			LngSet other = (LngSet) object;
			boolean b = size == other.size;
			for (long c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(LngSink sink) {
		LngSource source = source_();
		long c;
		while ((c = source.source()) != LngFunUtil.EMPTYVALUE)
			sink.sink(c);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (long c : streamlet())
			h = h * 31 + Long.hashCode(c);
		return h;
	}

	public LngSource source() {
		return source_();
	}

	public LngStreamlet streamlet() {
		return new LngStreamlet(() -> LngOutlet.of(source_()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (long c : streamlet())
			sb.append(c + ",");
		return sb.toString();
	}

	private boolean add_(long c) {
		int index = index(c);
		if (0 <= index) {
			vs[index] = c;
			return true;
		} else
			return false;
	}

	private int index(long c) {
		int mask = vs.length - 1;
		int index = Long.hashCode(c) & mask;
		long c0;
		while ((c0 = vs[index]) != LngFunUtil.EMPTYVALUE)
			if (c0 != c)
				index = index + 1 & mask;
			else
				return -1;
		return index;
	}

	private LngSource source_() {
		return new LngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public long source() {
				long v;
				while (index < capacity)
					if ((v = vs[index++]) != LngFunUtil.EMPTYVALUE)
						return v;
				return LngFunUtil.EMPTYVALUE;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
