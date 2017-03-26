package suite.primitive;

import java.io.CharArrayReader;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.util.Copy;
import suite.util.To;
import suite.util.Util;

public class Chars implements Iterable<Character> {

	private static char emptyArray[] = new char[0];
	private static int reallocSize = 65536;

	public static Chars empty = Chars.of(emptyArray);

	public final char cs[]; // immutable
	public final int start, end;

	public static Comparator<Chars> comparator = (chars0, chars1) -> {
		int start0 = chars0.start, start1 = chars1.start;
		int size0 = chars0.size(), size1 = chars1.size(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			char c0 = chars0.cs[start0 + index];
			char c1 = chars1.cs[start1 + index];
			c = c0 == c1 ? 0 : c0 < c1 ? -1 : 1;
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Chars of(IoSink<Writer> ioSink) throws IOException {
		Writer writer = new StringWriter();
		ioSink.sink(writer);
		return Chars.of(writer.toString());
	}

	public static Chars of(String s) {
		char[] a = To.charArray(s);
		return Chars.of(a);
	}

	public static Chars of(Chars chars) {
		return Chars.of(chars.cs, chars.start, chars.end);
	}

	public static Chars of(char... cs) {
		return Chars.of(cs, 0);
	}

	public static Chars of(char cs[], int start) {
		return Chars.of(cs, start, cs.length);
	}

	public static Chars of(char cs[], int start, int end) {
		return new Chars(cs, start, end);
	}

	public Chars(char cs[], int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Chars append(Chars a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		char nb[] = new char[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return Chars.of(nb);
	}

	public static Chars asList(char... in) {
		return Chars.of(in);
	}

	public static Chars concat(Chars... array) {
		CharsBuilder bb = new CharsBuilder();
		for (Chars chars : array)
			bb.append(chars);
		return bb.toChars();
	}

	public Reader asReader() {
		return new CharArrayReader(cs, start, end - start);
	}

	public char get(int index) {
		if (index < 0)
			index += size();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Chars chars, int start) {
		for (int i = start; i <= size() - chars.size(); i++)
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
			result &= Character.isWhitespace(cs[i]);
		return result;
	}

	public Chars pad(int size) {
		CharsBuilder cb = new CharsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append(' ');
		return cb.toChars();
	}

	public Chars replace(Chars from, Chars to) {
		CharsBuilder cb = new CharsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(subchars(i0, i));
			cb.append(to);
			i0 = i + from.size();
		}
		cb.append(subchars(i0));
		return cb.toChars();
	}

	public int size() {
		return end - start;
	}

	public boolean startsWith(Chars chars) {
		return startsWith(chars, 0);
	}

	public boolean startsWith(Chars chars, int s) {
		if (s + chars.size() <= size()) {
			boolean result = true;
			for (int i = 0; result && i < chars.size(); i++)
				result &= get(s + i) == chars.get(i);
			return result;
		} else
			return false;
	}

	public Chars subchars(int s) {
		return subchars0(start + s, end);
	}

	public Chars subchars(int s, int e) {
		int size = size();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		return subchars0(start + s, start + e);
	}

	public CharBuffer toCharBuffer() {
		return CharBuffer.wrap(cs, start, end - start);
	}

	public char[] toChars() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public Chars trim() {
		int s = start;
		int e = end;
		while (s < e && Character.isWhitespace(cs[s]))
			s++;
		while (s < e && Character.isWhitespace(cs[e - 1]))
			e--;
		return Chars.of(cs, s, e);
	}

	public void write(DataOutput dataOutput) throws IOException {
		dataOutput.writeChars(new String(toChars()));
	}

	public void write(Writer writer) throws IOException {
		writer.write(cs, start, end - start);
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
		if (Util.clazz(object) == Chars.class) {
			Chars other = (Chars) object;

			if (size() == other.size()) {
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
			result = 31 * result + cs[i];
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private Chars subchars0(int start, int end) {
		Chars result = new Chars(cs, start, end);

		// avoid small pack of chars object keeping a large buffer
		if (Boolean.FALSE && reallocSize <= cs.length && end - start < reallocSize / 4)
			result = empty.append(result); // do not share reference

		return result;
	}

	private void checkClosedBounds(int index) {
		if (index < start || end <= index)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "]");
	}

	public static class CharsBuilder {
		private char cs[] = emptyArray;
		private int size;

		public CharsBuilder append(Chars chars) {
			return append(chars.cs, chars.start, chars.end);
		}

		public CharsBuilder append(char c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public CharsBuilder append(char cs_[]) {
			return append(cs, 0, cs.length);
		}

		public CharsBuilder append(char cs_[], int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.primitiveArray(cs_, start, cs, size, inc);
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
			return new Chars(cs, 0, size);
		}

		private void extendBuffer(int capacity1) {
			int capacity0 = cs.length;

			if (capacity0 < capacity1) {
				int capacity = Math.max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < Constants.bufferSize ? capacity << 1 : capacity * 3 / 2;

				char chars1[] = new char[capacity];
				Copy.primitiveArray(cs, 0, chars1, 0, size);
				cs = chars1;
			}
		}
	}

}
