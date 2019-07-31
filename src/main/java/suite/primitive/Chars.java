package suite.primitive;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import primal.Ob;
import suite.cfg.Defaults;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Puller;
import suite.util.Compare;
import suite.util.ParseUtil;
import suite.util.String_;

public class Chars implements Iterable<Character> {

	private static char[] emptyArray = new char[0];
	private static int reallocSize = 65536;

	public static Chars empty = of(emptyArray);

	public final char[] cs; // immutable
	public final int start, end;

	public interface WriteChar {
		public void write(char[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Chars> comparator = (chars0, chars1) -> {
		int start0 = chars0.start, start1 = chars1.start;
		int size0 = chars0.size_(), size1 = chars1.size_(), minSize = min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			var c0 = chars0.cs[start0 + index];
			var c1 = chars1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Chars concat(Chars... array) {
		var bb = new CharsBuilder();
		for (var chars : array)
			bb.append(chars);
		return bb.toChars();
	}

	public static Chars of(Puller<Chars> puller) {
		var cb = new CharsBuilder();
		puller.forEach(cb::append);
		return cb.toChars();
	}

	public static Chars of(CharBuffer cb) {
		var offset = cb.arrayOffset();
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
		var nb = new char[newSize];
		Chars_.copy(cs, start, nb, 0, size0);
		Chars_.copy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public <T> T collect(Fun<Chars, T> fun) {
		return fun.apply(this);
	}

	public char get(int index) {
		if (index < 0)
			index += size_();
		var i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Chars chars, int start) {
		for (var i = start; i <= size_() - chars.size_(); i++)
			if (startsWith(chars, i))
				return i;
		return -1;
	}

	public boolean isEmpty() {
		return end <= start;
	}

	public boolean isWhitespaces() {
		var b = true;
		for (var i = start; b && i < end; i++)
			b &= ParseUtil.isWhitespace(cs[i]);
		return b;
	}

	public ChrStreamlet streamlet() {
		return new ChrStreamlet(() -> ChrPuller.of(new ChrSource() {
			private int i = start;

			public char g() {
				return i < end ? cs[i++] : ChrFunUtil.EMPTYVALUE;
			}
		}));
	}

	public Chars pad(int size) {
		var cb = new CharsBuilder();
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
		var cb = new CharsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toChars();
	}

	public Chars reverse() {
		var cs_ = new char[size_()];
		int si = start, di = 0;
		while (si < end)
			cs_[di++] = cs[si++];
		return Chars.of(cs_);

	}

	public int size() {
		return size_();
	}

	public Chars sort() {
		var cs = toArray();
		Arrays.sort(cs);
		return Chars.of(cs);
	}

	public boolean startsWith(Chars chars) {
		return startsWith_(chars, 0);
	}

	public boolean startsWith(Chars chars, int s) {
		return startsWith_(chars, s);
	}

	public char[] toArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public CharBuffer toCharBuffer() {
		return CharBuffer.wrap(cs, start, end - start);
	}

	public Chars trim() {
		var s = start;
		var e = end;
		while (s < e && ParseUtil.isWhitespace(cs[s]))
			s++;
		while (s < e && ParseUtil.isWhitespace(cs[e - 1]))
			e--;
		return of(cs, s, e);
	}

	public void write(WriteChar out) {
		ex(() -> {
			out.write(cs, start, end - start);
			return out;
		});
	}

	@Override
	public Iterator<Character> iterator() {
		return new Iterator<>() {
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
		if (Ob.clazz(object) == Chars.class) {
			var other = (Chars) object;

			if (size_() == other.size_()) {
				var diff = other.start - start;
				for (var i = start; i < end; i++)
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
		var h = 7;
		for (var i = start; i < end; i++)
			h = h * 31 + Character.hashCode(cs[i]);
		return h;
	}

	@Override
	public String toString() {
		return String_.build(sb -> {
			for (var i = start; i < end; i++)
				sb.append(cs[i]);
		});
	}

	private boolean startsWith_(Chars chars, int s) {
		if (s + chars.size_() <= size_()) {
			var b = true;
			for (var i = 0; b && i < chars.size_(); i++)
				b &= get(s + i) == chars.get(i);
			return b;
		} else
			return false;
	}

	private Chars range_(int s) {
		return range_(s, size_());
	}

	private Chars range_(int s, int e) {
		var size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = min(size, s);
		e = min(size, e);
		int start_ = start + min(size, s);
		int end_ = start + min(size, e);
		var result = of(cs, start_, end_);

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
			var inc = end - start;
			extendBuffer(size + inc);
			Chars_.copy(cs_, start, cs, size, inc);
			size += inc;
			return this;
		}

		public void clear() {
			cs = emptyArray;
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
			var capacity0 = cs.length;

			if (capacity0 < capacity1) {
				int capacity = max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < Defaults.bufferSize ? capacity << 1 : capacity * 3 / 2;

				cs = Arrays.copyOf(cs, capacity);
			}
		}
	}

}
