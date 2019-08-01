package suite.primitive;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import primal.Verbs.Compare;
import primal.Verbs.Get;
import primal.fp.Funs.Fun;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntSource;
import suite.cfg.Defaults;
import suite.primitive.streamlet.IntPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Puller;
import suite.util.ParseUtil;
import suite.util.To;

public class Ints implements Iterable<Integer> {

	private static int[] emptyArray = new int[0];
	private static int reallocSize = 65536;

	public static Ints empty = of(emptyArray);

	public final int[] cs; // immutable
	public final int start, end;

	public interface WriteChar {
		public void write(int[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Ints> comparator = (ints0, ints1) -> {
		int start0 = ints0.start, start1 = ints1.start;
		int size0 = ints0.size_(), size1 = ints1.size_(), minSize = min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			var c0 = ints0.cs[start0 + index];
			var c1 = ints1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Ints concat(Ints... array) {
		var bb = new IntsBuilder();
		for (var ints : array)
			bb.append(ints);
		return bb.toInts();
	}

	public static Ints of(Puller<Ints> puller) {
		var cb = new IntsBuilder();
		puller.forEach(cb::append);
		return cb.toInts();
	}

	public static Ints of(IntBuffer cb) {
		var offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Ints of(Ints ints) {
		return of(ints.cs, ints.start, ints.end);
	}

	public static Ints of(int... cs) {
		return of(cs, 0);
	}

	public static Ints of(int[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Ints of(int[] cs, int start, int end) {
		return new Ints(cs, start, end);
	}

	private Ints(int[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Ints append(Ints a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		var nb = new int[newSize];
		Ints_.copy(cs, start, nb, 0, size0);
		Ints_.copy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public <T> T collect(Fun<Ints, T> fun) {
		return fun.apply(this);
	}

	public int get(int index) {
		if (index < 0)
			index += size_();
		var i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Ints ints, int start) {
		for (var i = start; i <= size_() - ints.size_(); i++)
			if (startsWith(ints, i))
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

	public IntStreamlet streamlet() {
		return new IntStreamlet(() -> IntPuller.of(new IntSource() {
			private int i = start;

			public int g() {
				return i < end ? cs[i++] : IntPrim.EMPTYVALUE;
			}
		}));
	}

	public Ints pad(int size) {
		var cb = new IntsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((int) 0);
		return cb.toInts();
	}

	public Ints range(int s) {
		return range_(s);
	}

	public Ints range(int s, int e) {
		return range_(s, e);
	}

	public Ints replace(Ints from, Ints to) {
		var cb = new IntsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toInts();
	}

	public Ints reverse() {
		var cs_ = new int[size_()];
		int si = start, di = 0;
		while (si < end)
			cs_[di++] = cs[si++];
		return Ints.of(cs_);

	}

	public int size() {
		return size_();
	}

	public Ints sort() {
		var cs = toArray();
		Arrays.sort(cs);
		return Ints.of(cs);
	}

	public boolean startsWith(Ints ints) {
		return startsWith_(ints, 0);
	}

	public boolean startsWith(Ints ints, int s) {
		return startsWith_(ints, s);
	}

	public int[] toArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public IntBuffer toIntBuffer() {
		return IntBuffer.wrap(cs, start, end - start);
	}

	public Ints trim() {
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
	public Iterator<Integer> iterator() {
		return new Iterator<>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Integer next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Ints.class) {
			var other = (Ints) object;

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
			h = h * 31 + Integer.hashCode(cs[i]);
		return h;
	}

	@Override
	public String toString() {
		return To.string(sb -> {
			for (var i = start; i < end; i++)
				sb.append(cs[i]);
		});
	}

	private boolean startsWith_(Ints ints, int s) {
		if (s + ints.size_() <= size_()) {
			var b = true;
			for (var i = 0; b && i < ints.size_(); i++)
				b &= get(s + i) == ints.get(i);
			return b;
		} else
			return false;
	}

	private Ints range_(int s) {
		return range_(s, size_());
	}

	private Ints range_(int s, int e) {
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

		// avoid small pack of ints object keeping a large buffer
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

	public static class IntsBuilder {
		private int[] cs = emptyArray;
		private int size;

		public IntsBuilder append(Ints ints) {
			return append(ints.cs, ints.start, ints.end);
		}

		public IntsBuilder append(int c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public IntsBuilder append(int[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public IntsBuilder append(int[] cs_, int start, int end) {
			var inc = end - start;
			extendBuffer(size + inc);
			Ints_.copy(cs_, start, cs, size, inc);
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

		public Ints toInts() {
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
