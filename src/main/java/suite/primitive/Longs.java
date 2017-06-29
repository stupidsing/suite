package suite.primitive;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.util.Compare;
import suite.util.Copy;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;

public class Longs implements Iterable<Long> {

	private static long[] emptyArray = new long[0];
	private static int reallocSize = 65536;

	public static Longs empty = of(emptyArray);

	public final long[] cs; // immutable
	public final int start, end;

	@FunctionalInterface
	public interface WriteChar {
		public void write(long[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Longs> comparator = (longs0, longs1) -> {
		int start0 = longs0.start, start1 = longs1.start;
		int size0 = longs0.size_(), size1 = longs1.size_(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			long c0 = longs0.cs[start0 + index];
			long c1 = longs1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Longs of(LongBuffer cb) {
		int offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Longs of(Longs longs) {
		return of(longs.cs, longs.start, longs.end);
	}

	public static Longs of(long... cs) {
		return of(cs, 0);
	}

	public static Longs of(long[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Longs of(long[] cs, int start, int end) {
		return new Longs(cs, start, end);
	}

	private Longs(long[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Longs append(Longs a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		long[] nb = new long[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public static Longs asList(long... in) {
		return of(in);
	}

	public static Longs concat(Longs... array) {
		LongsBuilder bb = new LongsBuilder();
		for (Longs longs : array)
			bb.append(longs);
		return bb.toLongs();
	}

	public <T> T collect(Fun<Longs, T> fun) {
		return fun.apply(this);
	}

	public long get(int index) {
		if (index < 0)
			index += size_();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Longs longs, int start) {
		for (int i = start; i <= size_() - longs.size_(); i++)
			if (startsWith(longs, i))
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

	public LngStreamlet streamlet() {
		return new LngStreamlet(() -> LngOutlet.of(new LngSource() {
			private int i = start;

			public long source() {
				return i < end ? cs[i++] : LngFunUtil.EMPTYVALUE;
			}
		}));
	}

	public Longs pad(int size) {
		LongsBuilder cb = new LongsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((long) 0);
		return cb.toLongs();
	}

	public Longs range(int s) {
		return range_(s);
	}

	public Longs range(int s, int e) {
		return range_(s, e);
	}

	public Longs replace(Longs from, Longs to) {
		LongsBuilder cb = new LongsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toLongs();
	}

	public Longs reverse() {
		long[] cs_ = new long[size_()];
		int si = start, di = 0;
		while (si < end)
			cs_[di++] = cs[si++];
		return Longs.of(cs_);

	}

	public int size() {
		return size_();
	}

	public Longs sort() {
		long[] cs = toArray();
		Arrays.sort(cs);
		return Longs.of(cs);
	}

	public boolean startsWith(Longs longs) {
		return startsWith_(longs, 0);
	}

	public boolean startsWith(Longs longs, int s) {
		return startsWith_(longs, s);
	}

	public long[] toArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public LongBuffer toLongBuffer() {
		return LongBuffer.wrap(cs, start, end - start);
	}

	public Longs trim() {
		int s = start;
		int e = end;
		while (s < e && ParseUtil.isWhitespace(cs[s]))
			s++;
		while (s < e && ParseUtil.isWhitespace(cs[e - 1]))
			e--;
		return of(cs, s, e);
	}

	public void write(WriteChar out) {
		try {
			out.write(cs, start, end - start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Iterator<Long> iterator() {
		return new Iterator<Long>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Long next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Longs.class) {
			Longs other = (Longs) object;

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
			result = 31 * result + Long.hashCode(cs[i]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private boolean startsWith_(Longs longs, int s) {
		if (s + longs.size_() <= size_()) {
			boolean result = true;
			for (int i = 0; result && i < longs.size_(); i++)
				result &= get(s + i) == longs.get(i);
			return result;
		} else
			return false;
	}

	private Longs range_(int s) {
		return range_(s, size_());
	}

	private Longs range_(int s, int e) {
		int size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		int start_ = start + Math.min(size, s);
		int end_ = start + Math.min(size, e);
		Longs result = of(cs, start_, end_);

		// avoid small pack of longs object keeping a large buffer
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

	public static class LongsBuilder {
		private long[] cs = emptyArray;
		private int size;

		public LongsBuilder append(Longs longs) {
			return append(longs.cs, longs.start, longs.end);
		}

		public LongsBuilder append(long c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public LongsBuilder append(long[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public LongsBuilder append(long[] cs_, int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.longs(cs_, start, cs, size, inc);
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

		public Longs toLongs() {
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
