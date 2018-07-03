package suite.primitive;

import static suite.util.Friends.max;
import static suite.util.Friends.min;
import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.cfg.Defaults;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Outlet;
import suite.util.Compare;
import suite.util.Object_;
import suite.util.ParseUtil;

public class Floats implements Iterable<Float> {

	private static float[] emptyArray = new float[0];
	private static int reallocSize = 65536;

	public static Floats empty = of(emptyArray);

	public final float[] cs; // immutable
	public final int start, end;

	public interface WriteChar {
		public void write(float[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Floats> comparator = (floats0, floats1) -> {
		int start0 = floats0.start, start1 = floats1.start;
		int size0 = floats0.size_(), size1 = floats1.size_(), minSize = min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			var c0 = floats0.cs[start0 + index];
			var c1 = floats1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Floats concat(Floats... array) {
		var bb = new FloatsBuilder();
		for (var floats : array)
			bb.append(floats);
		return bb.toFloats();
	}

	public static Floats of(Outlet<Floats> outlet) {
		var cb = new FloatsBuilder();
		outlet.forEach(cb::append);
		return cb.toFloats();
	}

	public static Floats of(FloatBuffer cb) {
		var offset = cb.arrayOffset();
		return of(cb.array(), offset, offset + cb.limit());
	}

	public static Floats of(Floats floats) {
		return of(floats.cs, floats.start, floats.end);
	}

	public static Floats of(float... cs) {
		return of(cs, 0);
	}

	public static Floats of(float[] cs, int start) {
		return of(cs, start, cs.length);
	}

	public static Floats of(float[] cs, int start, int end) {
		return new Floats(cs, start, end);
	}

	private Floats(float[] cs, int start, int end) {
		this.cs = cs;
		this.start = start;
		this.end = end;
	}

	public Floats append(Floats a) {
		int size0 = size_(), size1 = a.size_(), newSize = size0 + size1;
		var nb = new float[newSize];
		Floats_.copy(cs, start, nb, 0, size0);
		Floats_.copy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public <T> T collect(Fun<Floats, T> fun) {
		return fun.apply(this);
	}

	public float get(int index) {
		if (index < 0)
			index += size_();
		var i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Floats floats, int start) {
		for (var i = start; i <= size_() - floats.size_(); i++)
			if (startsWith(floats, i))
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

	public FltStreamlet streamlet() {
		return new FltStreamlet(() -> FltOutlet.of(new FltSource() {
			private int i = start;

			public float source() {
				return i < end ? cs[i++] : FltFunUtil.EMPTYVALUE;
			}
		}));
	}

	public Floats pad(int size) {
		var cb = new FloatsBuilder();
		cb.append(this);
		while (cb.size() < size)
			cb.append((float) 0);
		return cb.toFloats();
	}

	public Floats range(int s) {
		return range_(s);
	}

	public Floats range(int s, int e) {
		return range_(s, e);
	}

	public Floats replace(Floats from, Floats to) {
		var cb = new FloatsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toFloats();
	}

	public Floats reverse() {
		var cs_ = new float[size_()];
		int si = start, di = 0;
		while (si < end)
			cs_[di++] = cs[si++];
		return Floats.of(cs_);

	}

	public int size() {
		return size_();
	}

	public Floats sort() {
		var cs = toArray();
		Arrays.sort(cs);
		return Floats.of(cs);
	}

	public boolean startsWith(Floats floats) {
		return startsWith_(floats, 0);
	}

	public boolean startsWith(Floats floats, int s) {
		return startsWith_(floats, s);
	}

	public float[] toArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public FloatBuffer toFloatBuffer() {
		return FloatBuffer.wrap(cs, start, end - start);
	}

	public Floats trim() {
		var s = start;
		var e = end;
		while (s < e && ParseUtil.isWhitespace(cs[s]))
			s++;
		while (s < e && ParseUtil.isWhitespace(cs[e - 1]))
			e--;
		return of(cs, s, e);
	}

	public void write(WriteChar out) {
		rethrow(() -> {
			out.write(cs, start, end - start);
			return out;
		});
	}

	@Override
	public Iterator<Float> iterator() {
		return new Iterator<>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Float next() {
				return cs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Floats.class) {
			var other = (Floats) object;

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
			h = h * 31 + Float.hashCode(cs[i]);
		return h;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private boolean startsWith_(Floats floats, int s) {
		if (s + floats.size_() <= size_()) {
			var b = true;
			for (var i = 0; b && i < floats.size_(); i++)
				b &= get(s + i) == floats.get(i);
			return b;
		} else
			return false;
	}

	private Floats range_(int s) {
		return range_(s, size_());
	}

	private Floats range_(int s, int e) {
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

		// avoid small pack of floats object keeping a large buffer
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

	public static class FloatsBuilder {
		private float[] cs = emptyArray;
		private int size;

		public FloatsBuilder append(Floats floats) {
			return append(floats.cs, floats.start, floats.end);
		}

		public FloatsBuilder append(float c) {
			extendBuffer(size + 1);
			cs[size++] = c;
			return this;
		}

		public FloatsBuilder append(float[] cs_) {
			return append(cs_, 0, cs_.length);
		}

		public FloatsBuilder append(float[] cs_, int start, int end) {
			var inc = end - start;
			extendBuffer(size + inc);
			Floats_.copy(cs_, start, cs, size, inc);
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

		public Floats toFloats() {
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
