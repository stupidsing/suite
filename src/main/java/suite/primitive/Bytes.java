package suite.primitive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.Constants;
import suite.util.Copy;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.To;

public class Bytes implements Iterable<Byte> {

	private static byte[] emptyArray = new byte[0];
	private static int reallocSize = 65536;

	public static Bytes empty = of(emptyArray);

	public final byte[] bs; // immutable
	public final int start, end;

	@FunctionalInterface
	public interface Writer {
		public void write(byte[] bs, int offset, int length) throws IOException;
	};

	public static Comparator<Bytes> comparator = (bytes0, bytes1) -> {
		int start0 = bytes0.start, start1 = bytes1.start;
		int size0 = bytes0.size(), size1 = bytes1.size(), minSize = Math.min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			int b0 = Byte.toUnsignedInt(bytes0.bs[start0 + index]);
			int b1 = Byte.toUnsignedInt(bytes1.bs[start1 + index]);
			c = Integer.compare(b0, b1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Bytes of(ByteBuffer bb) {
		int offset = bb.arrayOffset();
		return of(bb.array(), offset, offset + bb.limit());
	}

	public static Bytes of(Bytes bytes) {
		return of(bytes.bs, bytes.start, bytes.end);
	}

	public static Bytes of(byte... bs) {
		return of(bs, 0);
	}

	public static Bytes of(byte[] bs, int start) {
		return of(bs, start, bs.length);
	}

	public static Bytes of(byte[] bs, int start, int end) {
		return new Bytes(bs, start, end);
	}

	private Bytes(byte[] bs, int start, int end) {
		this.bs = bs;
		this.start = start;
		this.end = end;
	}

	public Bytes append(Bytes a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		byte[] nb = new byte[newSize];
		System.arraycopy(bs, start, nb, 0, size0);
		System.arraycopy(a.bs, a.start, nb, size0, size1);
		return of(nb);
	}

	public static Bytes asList(byte... in) {
		return of(in);
	}

	public static Bytes concat(Bytes... array) {
		BytesBuilder bb = new BytesBuilder();
		for (Bytes bytes : array)
			bb.append(bytes);
		return bb.toBytes();
	}

	public <T> T collect(Fun<Bytes, T> fun) {
		return fun.apply(this);
	}

	public byte get(int index) {
		if (index < 0)
			index += size();
		int i1 = index + start;
		checkClosedBounds(i1);
		return bs[i1];
	}

	public int indexOf(Bytes bytes, int start) {
		for (int i = start; i <= size() - bytes.size(); i++)
			if (startsWith(bytes, i))
				return i;
		return -1;
	}

	public boolean isEmpty() {
		return end <= start;
	}

	public boolean isZeroes() {
		boolean result = true;
		for (int i = start; result && i < end; i++)
			result &= bs[i] == 0;
		return result;
	}

	public Bytes pad(int size) {
		BytesBuilder bb = new BytesBuilder();
		bb.append(this);
		while (bb.size() < size)
			bb.append((byte) 0);
		return bb.toBytes();
	}

	public Bytes range(int s) {
		return range_(s);
	}

	public Bytes range(int s, int e) {
		return range_(s, e);
	}

	public Bytes replace(Bytes from, Bytes to) {
		BytesBuilder bb = new BytesBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			bb.append(range_(i0, i));
			bb.append(to);
			i0 = i + from.size();
		}
		bb.append(range_(i0));
		return bb.toBytes();
	}

	public int size() {
		return end - start;
	}

	public boolean startsWith(Bytes bytes) {
		return startsWith_(bytes, 0);
	}

	public boolean startsWith(Bytes bytes, int s) {
		return startsWith_(bytes, s);
	}

	public byte[] toByteArray() {
		if (start != 0 || end != bs.length)
			return Arrays.copyOfRange(bs, start, end);
		else
			return bs;
	}

	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(bs, start, end - start);
	}

	public Bytes trim() {
		int s = start;
		int e = end;
		while (s < e && bs[s] == 0)
			s++;
		while (s < e && bs[e - 1] == 0)
			e--;
		return of(bs, s, e);
	}

	public void write(Writer out) {
		try {
			out.write(bs, start, end - start);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Iterator<Byte> iterator() {
		return new Iterator<Byte>() {
			private int pos = start;

			public boolean hasNext() {
				return pos < end;
			}

			public Byte next() {
				return bs[pos++];
			}
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Bytes.class) {
			Bytes other = (Bytes) object;

			if (end - start == other.end - other.start) {
				int diff = other.start - start;
				for (int i = start; i < end; i++)
					if (bs[i] != other.bs[i + diff])
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
			result = 31 * result + bs[i];
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(" " + To.hex2(bs[i]));
		return sb.toString();
	}

	private boolean startsWith_(Bytes bytes, int s) {
		if (s + bytes.size() <= size()) {
			boolean result = true;
			for (int i = 0; result && i < bytes.size(); i++)
				result &= get(s + i) == bytes.get(i);
			return result;
		} else
			return false;
	}

	private Bytes range_(int s) {
		return range_(s, size());
	}

	private Bytes range_(int s, int e) {
		int size = size();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = Math.min(size, s);
		e = Math.min(size, e);
		int start_ = start + Math.min(size, s);
		int end_ = start + Math.min(size, e);
		Bytes result = of(bs, start_, end_);

		// avoid small pack of bytes object keeping a large buffer
		if (Boolean.FALSE && reallocSize <= bs.length && end_ - start_ < reallocSize / 4)
			result = empty.append(result); // do not share reference

		return result;
	}

	private void checkClosedBounds(int index) {
		if (index < start || end <= index)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "]");
	}

	public static class BytesBuilder {
		private byte[] bs = emptyArray;
		private int size;

		public BytesBuilder append(Bytes bytes) {
			return append(bytes.bs, bytes.start, bytes.end);
		}

		public BytesBuilder append(byte b) {
			extendBuffer(size + 1);
			bs[size++] = b;
			return this;
		}

		public BytesBuilder append(byte[] bs_) {
			return append(bs_, 0, bs_.length);
		}

		public BytesBuilder append(byte[] bs_, int start, int end) {
			int inc = end - start;
			extendBuffer(size + inc);
			Copy.primitiveArray(bs_, start, bs, size, inc);
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

		public Bytes toBytes() {
			return of(bs, 0, size);
		}

		private void extendBuffer(int capacity1) {
			int capacity0 = bs.length;

			if (capacity0 < capacity1) {
				int capacity = Math.max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < Constants.bufferSize ? capacity << 1 : capacity * 3 / 2;

				bs = Arrays.copyOf(bs, capacity);
			}
		}
	}

}
