package suite.primitive;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.util.Compare;
import suite.util.Copy;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;

public class Chars implements Iterable<Character> {

	private static char[] emptyArray = new char[0];
	private static int reallocSize = 65536;

	public static Chars empty = of(emptyArray);

	public final char[] cs; // immutable
	public final int start, end;

	@FunctionalInterface
	public interface WriteChar {
		public void write(char[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Chars> comparator = (chars0, chars1) -> {
		int start0 = chars0.start, start1 = chars1.start;
		int size0 = chars0.size_(), size1 = chars1.size_(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			char c0 = chars0.cs[start0 + index];
			char c1 = chars1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Chars of(CharBuffer cb) {
		int offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Chars of(Chars chars) {
		return of(chars.cs, chars.start, chars.end);
	}

	public static Chars of(char... cs) {
		return of(cs, 0);
	}

	public static Chars of(char[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Chars of(char[] cs, int start, int end) {
		return new Chars(cs, start, end);
	}

	private Chars(char[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Chars append(Chars a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		char[] nb = new char[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public static Chars asList(char... in) {
		return of(in);
	}

	public static Chars concat(Chars... array) {
		CharsBuilder bb = new CharsBuilder();
		for (Chars chars : array)
			bb.append(chars);
		return bb.toChars();
	}

	public <T> T collect(Fun<Chars, T> fun) {
		return fun.apply(this);
	}

	public char get(int index) {
		if (index < 0)
			index += size_();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Chars chars, int start) {
		for (int i = start; i <= size_() - chars.size_(); i++)
			if (startsWith(chars, i))
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

	public Chars pad(int size) {
		CharsBuilder cb = new CharsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((char) 0);
		return cb.toChars();
	}

	public Chars range(int s) {
		return range_(s);
	}

	public Chars range(int s, int e) {
		return range_(s, e);
	}

	public Chars replace(Chars from, Chars to) {
		CharsBuilder cb = new CharsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toChars();
	}

	public int size() {
		return size_();
	}

	public boolean startsWith(Chars chars) {
		return startsWith_(chars, 0);
	}

	public boolean startsWith(Chars chars, int s) {
		return startsWith_(chars, s);
	}

	public char[] toCharArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public CharBuffer toCharBuffer() {
		return CharBuffer.wrap(cs, start, end - start);
	}

	public Chars trim() {
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
	public Iterator<Character> iterator() {
		return new Iterator<Character>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Character next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Chars.class) {
			Chars other = (Chars) object;

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
			result = 31 * result + Character.hashCode(cs[i]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private boolean startsWith_(Chars chars, int s) {
		if (s + chars.size_() <= size_()) {
			boolean result = true;
			for (int i = 0; result && i < chars.size_(); i++)
				result &= get(s + i) == chars.get(i);
			return result;
		} else
			return false;
	}

	private Chars range_(int s) {
		return range_(s, size_());
	}

	private Chars range_(int s, int e) {
		int size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		int start_ = start + Math.min(size, s);
		int end_ = start + Math.min(size, e);
		Chars result = of(cs, start_, end_);

		// avoid small pack of chars object keeping a large buffer
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

	public static class CharsBuilder {
		private char[] cs = emptyArray;
		private int size;

		public CharsBuilder append(Chars chars) {
			return append(chars.cs, chars.start, chars.end);
		}

		public CharsBuilder append(char c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public CharsBuilder append(char[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public CharsBuilder append(char[] cs_, int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.chars(cs_, start, cs, size, inc);
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

		public Chars toChars() {
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
