package suite.primitive;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.util.Copy;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;
import suite.util.To;

public class Shorts implements Iterable<Short> {

	private static short[] emptyArray = new short[0];
	private static int reallocSize = 65536;

	public static Shorts empty = of(emptyArray);

	public final short[] cs; // immutable
	public final int start, end;

	@FunctionalInterface
	public interface WriteShort {
		public void write(short[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Shorts> comparator = (shorts0, shorts1) -> {
		int start0 = shorts0.start, start1 = shorts1.start;
		int size0 = shorts0.size_(), size1 = shorts1.size_(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			int i0 = To.int_(shorts0.cs[start0 + index]);
			int i1 = To.int_(shorts1.cs[start1 + index]);
			c = Integer.compare(i0, i1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Shorts of(ShortBuffer cb) {
		int offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Shorts of(Shorts shorts) {
		return of(shorts.cs, shorts.start, shorts.end);
	}

	public static Shorts of(short... cs) {
		return of(cs, 0);
	}

	public static Shorts of(short[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Shorts of(short[] cs, int start, int end) {
		return new Shorts(cs, start, end);
	}

	private Shorts(short[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Shorts append(Shorts a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		short[] nb = new short[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public static Shorts asList(short... in) {
		return of(in);
	}

	public static Shorts concat(Shorts... array) {
		ShortsBuilder bb = new ShortsBuilder();
		for (Shorts shorts : array)
			bb.append(shorts);
		return bb.toShorts();
	}

	public <T> T collect(Fun<Shorts, T> fun) {
		return fun.apply(this);
	}

	public short get(int index) {
		if (index < 0)
			index += size_();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Shorts shorts, int start) {
		for (int i = start; i <= size_() - shorts.size_(); i++)
			if (startsWith(shorts, i))
				return i;
		return -1;
	}

	public boolean isEmpty() {
		return end <= start;
	}

	public boolean isWhitespaces() {
		boolean result = true;
		for (int i = start; result && i < end; i++)
			result &= ParseUtil.isWhitespace(cs[i]);
		return result;
	}

	public Shorts pad(int size) {
		ShortsBuilder cb = new ShortsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((short) 0);
		return cb.toShorts();
	}

	public Shorts range(int s) {
		return range_(s);
	}

	public Shorts range(int s, int e) {
		return range_(s, e);
	}

	public Shorts replace(Shorts from, Shorts to) {
		ShortsBuilder cb = new ShortsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toShorts();
	}

	public int size() {
		return size_();
	}

	public boolean startsWith(Shorts shorts) {
		return startsWith_(shorts, 0);
	}

	public boolean startsWith(Shorts shorts, int s) {
		return startsWith_(shorts, s);
	}

	public short[] toShortArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public ShortBuffer toShortBuffer() {
		return ShortBuffer.wrap(cs, start, end - start);
	}

	public Shorts trim() {
		int s = start;
		int e = end;
		while (s < e && ParseUtil.isWhitespace(cs[s]))
			s++;
		while (s < e && ParseUtil.isWhitespace(cs[e - 1]))
			e--;
		return of(cs, s, e);
	}

	public void write(WriteShort out) {
		try {
			out.write(cs, start, end - start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Iterator<Short> iterator() {
		return new Iterator<Short>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Short next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Shorts.class) {
			Shorts other = (Shorts) object;

			if (size_() == other.size_()) {
				int diff = other.start - start;
				for (int i = start; i < end; i++)
					if (cs[i] != other.cs[i + diff])
						return false;
				return true;
			} else
				return false;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++)
			result = 31 * result + Short.hashCode(cs[i]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private boolean startsWith_(Shorts shorts, int s) {
		if (s + shorts.size_() <= size_()) {
			boolean result = true;
			for (int i = 0; result && i < shorts.size_(); i++)
				result &= get(s + i) == shorts.get(i);
			return result;
		} else
			return false;
	}

	private Shorts range_(int s) {
		return range_(s, size_());
	}

	private Shorts range_(int s, int e) {
		int size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		int start_ = start + Math.min(size, s);
		int end_ = start + Math.min(size, e);
		Shorts result = of(cs, start_, end_);

		// avoid small pack of shorts object keeping a large buffer
		if (Boolean.FALSE && reallocSize <= cs.length && end_ - start_ < reallocSize / 4)
			result = empty.append(result); // do not share reference

		return result;
	}

	private void checkClosedBounds(int index) {
		if (index < start || end <= index)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "]");
	}

	private int size_() {
		return end - start;
	}

	public static class ShortsBuilder {
		private short[] cs = emptyArray;
		private int size;

		public ShortsBuilder append(Shorts shorts) {
			return append(shorts.cs, shorts.start, shorts.end);
		}

		public ShortsBuilder append(short c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public ShortsBuilder append(short[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public ShortsBuilder append(short[] cs_, int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.shorts(cs_, start, cs, size, inc);
			size += inc;
			return this;
		}

		public void clear() {
			size = 0;
		}

		public void extend(int size1) {
			extendBuffer(size1);
			size = size1;
		}

		public int size() {
			return size;
		}

		public Shorts toShorts() {
			return of(cs, 0, size);
		}

		private void extendBuffer(int capacity1) {
			int capacity0 = cs.length;

			if (capacity0 < capacity1) {
				int capacity = Math.max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < Constants.bufferSize ? capacity << 1 : capacity * 3 / 2;

				cs = Arrays.copyOf(cs, capacity);
			}
		}
	}

}
