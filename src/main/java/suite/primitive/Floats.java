package suite.primitive;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.util.Compare;
import suite.util.Copy;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.ParseUtil;

public class Floats implements Iterable<Float> {

	private static float[] emptyArray = new float[0];
	private static int reallocSize = 65536;

	public static Floats empty = of(emptyArray);

	public final float[] cs; // immutable
	public final int start, end;

	@FunctionalInterface
	public interface WriteFloat {
		public void write(float[] cs, int offset, int length) throws IOException;
	};

	public static Comparator<Floats> comparator = (floats0, floats1) -> {
		int start0 = floats0.start, start1 = floats1.start;
		int size0 = floats0.size_(), size1 = floats1.size_(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			float c0 = floats0.cs[start0 + index];
			float c1 = floats1.cs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Floats of(FloatBuffer cb) {
		int offset = cb.arrayOffset();
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
		float[] nb = new float[newSize];
		System.arraycopy(cs, start, nb, 0, size0);
		System.arraycopy(a.cs, a.start, nb, size0, size1);
		return of(nb);
	}

	public static Floats asList(float... in) {
		return of(in);
	}

	public static Floats concat(Floats... array) {
		FloatsBuilder bb = new FloatsBuilder();
		for (Floats floats : array)
			bb.append(floats);
		return bb.toFloats();
	}

	public <T> T collect(Fun<Floats, T> fun) {
		return fun.apply(this);
	}

	public float get(int index) {
		if (index < 0)
			index += size_();
		int i1 = index + start;
		checkClosedBounds(i1);
		return cs[i1];
	}

	public int indexOf(Floats floats, int start) {
		for (int i = start; i <= size_() - floats.size_(); i++)
			if (startsWith(floats, i))
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

	public Floats pad(int size) {
		FloatsBuilder cb = new FloatsBuilder();
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
		FloatsBuilder cb = new FloatsBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			cb.append(range_(i0, i));
			cb.append(to);
			i0 = i + from.size_();
		}
		cb.append(range_(i0));
		return cb.toFloats();
	}

	public int size() {
		return size_();
	}

	public boolean startsWith(Floats floats) {
		return startsWith_(floats, 0);
	}

	public boolean startsWith(Floats floats, int s) {
		return startsWith_(floats, s);
	}

	public float[] toFloatArray() {
		if (start != 0 || end != cs.length)
			return Arrays.copyOfRange(cs, start, end);
		else
			return cs;
	}

	public FloatBuffer toFloatBuffer() {
		return FloatBuffer.wrap(cs, start, end - start);
	}

	public Floats trim() {
		int s = start;
		int e = end;
		while (s < e && ParseUtil.isWhitespace(cs[s]))
			s++;
		while (s < e && ParseUtil.isWhitespace(cs[e - 1]))
			e--;
		return of(cs, s, e);
	}

	public void write(WriteFloat out) {
		try {
			out.write(cs, start, end - start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Iterator<Float> iterator() {
		return new Iterator<Float>() {
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
			Floats other = (Floats) object;

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
			result = 31 * result + Float.hashCode(cs[i]);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(cs[i]);
		return sb.toString();
	}

	private boolean startsWith_(Floats floats, int s) {
		if (s + floats.size_() <= size_()) {
			boolean result = true;
			for (int i = 0; result && i < floats.size_(); i++)
				result &= get(s + i) == floats.get(i);
			return result;
		} else
			return false;
	}

	private Floats range_(int s) {
		return range_(s, size_());
	}

	private Floats range_(int s, int e) {
		int size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		int start_ = start + Math.min(size, s);
		int end_ = start + Math.min(size, e);
		Floats result = of(cs, start_, end_);

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
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.floats(cs_, start, cs, size, inc);
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
