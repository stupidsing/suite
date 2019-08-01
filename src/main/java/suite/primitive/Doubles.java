package suite.primitive;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import primal.Verbs.Get;
import primal.fp.Funs.Fun;
import primal.primitive.DblPrim;
import primal.primitive.DblPrim.DblSource;
import suite.cfg.Defaults;
import suite.primitive.streamlet.DblPuller;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.Puller;
import suite.util.Compare;
import suite.util.ParseUtil;
import suite.util.To;

public class Doubles implements Iterable<Double> {

	private static double[] emptyArray = new double[0];
	private static int reallocSize = 65536;

	public static Doubles empty = of(emptyArray);

	public final double[] cs; // immutable
	public final int start, end;

	public interface WriteChar {
		public void write(double[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Doubles> comparator = (doubles0, doubles1) -> {
		int start0 = doubles0.start, start1 = doubles1.start;
		int size0 = doubles0.size_(), size1 = doubles1.size_(), minSize = min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			var c0 = doubles0.cs[start0 + index];
			var c1 = doubles1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Doubles concat(Doubles... array) {
		var bb = new DoublesBuilder();
		for (var doubles : array)
			bb.append(doubles);
		return bb.toDoubles();
	}

	public static Doubles of(Puller<Doubles> puller) {
		var cb = new DoublesBuilder();
		puller.forEach(cb::append);
		return cb.toDoubles();
	}

	public static Doubles of(DoubleBuffer cb) {
		var offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Doubles of(Doubles doubles) {
		return of(doubles.cs, doubles.start, doubles.end);
	}

	public static Doubles of(double... cs) {
		return of(cs, 0);
	}

	public static Doubles of(double[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Doubles of(double[] cs, int start, int end) {
		return new Doubles(cs, start, end);
	}

	private Doubles(double[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Doubles append(Doubles a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		var nb = new double[newSize];
		Doubles_.copy(cs, start, nb, 0, size0);
		Doubles_.copy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public <T> T collect(Fun<Doubles, T> fun) {
		return fun.apply(this);
	}

	public double get(int index) {
		if (index < 0)
			index += size_();
		var i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Doubles doubles, int start) {
		for (var i = start; i <= size_() - doubles.size_(); i++)
			if (startsWith(doubles, i))
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

	public DblStreamlet streamlet() {
		return new DblStreamlet(() -> DblPuller.of(new DblSource() {
			private int i = start;

			public double g() {
				return i < end ? cs[i++] : DblPrim.EMPTYVALUE;
			}
		}));
	}

	public Doubles pad(int size) {
		var cb = new DoublesBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((double) 0);
		return cb.toDoubles();
	}

	public Doubles range(int s) {
		return range_(s);
	}

	public Doubles range(int s, int e) {
		return range_(s, e);
	}

	public Doubles replace(Doubles from, Doubles to) {
		var cb = new DoublesBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toDoubles();
	}

	public Doubles reverse() {
		var cs_ = new double[size_()];
		int si = start, di = 0;
		while (si < end)
			cs_[di++] = cs[si++];
		return Doubles.of(cs_);

	}

	public int size() {
		return size_();
	}

	public Doubles sort() {
		var cs = toArray();
		Arrays.sort(cs);
		return Doubles.of(cs);
	}

	public boolean startsWith(Doubles doubles) {
		return startsWith_(doubles, 0);
	}

	public boolean startsWith(Doubles doubles, int s) {
		return startsWith_(doubles, s);
	}

	public double[] toArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public DoubleBuffer toDoubleBuffer() {
		return DoubleBuffer.wrap(cs, start, end - start);
	}

	public Doubles trim() {
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
	public Iterator<Double> iterator() {
		return new Iterator<>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Double next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Doubles.class) {
			var other = (Doubles) object;

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
			h = h * 31 + Double.hashCode(cs[i]);
		return h;
	}

	@Override
	public String toString() {
		return To.string(sb -> {
			for (var i = start; i < end; i++)
				sb.append(cs[i]);
		});
	}

	private boolean startsWith_(Doubles doubles, int s) {
		if (s + doubles.size_() <= size_()) {
			var b = true;
			for (var i = 0; b && i < doubles.size_(); i++)
				b &= get(s + i) == doubles.get(i);
			return b;
		} else
			return false;
	}

	private Doubles range_(int s) {
		return range_(s, size_());
	}

	private Doubles range_(int s, int e) {
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

		// avoid small pack of doubles object keeping a large buffer
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

	public static class DoublesBuilder {
		private double[] cs = emptyArray;
		private int size;

		public DoublesBuilder append(Doubles doubles) {
			return append(doubles.cs, doubles.start, doubles.end);
		}

		public DoublesBuilder append(double c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public DoublesBuilder append(double[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public DoublesBuilder append(double[] cs_, int start, int end) {
			var inc = end - start;
			extendBuffer(size + inc);
			Doubles_.copy(cs_, start, cs, size, inc);
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

		public Doubles toDoubles() {
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
