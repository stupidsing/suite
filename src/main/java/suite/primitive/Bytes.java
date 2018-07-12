package suite.primitive;

import static suite.util.Friends.fail;
import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import suite.cfg.Defaults;
import suite.object.Object_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Outlet;
import suite.util.Compare;
import suite.util.To;

public class Bytes implements Iterable<Byte> {

	private static byte[] emptyArray = new byte[0];
	private static int reallocSize = 65536;

	public static Bytes empty = of(emptyArray);

	public final byte[] bs; // immutable
	public final int start, end;

	public interface WriteByte {
		public void write(byte[] bs, int offset, int length) throws IOException;
	};

	public static Comparator<Bytes> comparator = (bytes0, bytes1) -> {
		int start0 = bytes0.start, start1 = bytes1.start;
		int size0 = bytes0.size_(), size1 = bytes1.size_(), minSize = min(size0, size1);
		int index = 0, c = 0;

		while (c == 0 && index < minSize) {
			var c0 = bytes0.bs[start0 + index];
			var c1 = bytes1.bs[start1 + index];
			c = Compare.compare(c0, c1);
			index++;
		}

		return c != 0 ? c : size0 - size1;
	};

	public static Bytes concat(Bytes... array) {
		var bb = new BytesBuilder();
		for (var bytes : array)
			bb.append(bytes);
		return bb.toBytes();
	}

	public static Bytes of(Outlet<Bytes> outlet) {
		var bb = new BytesBuilder();
		outlet.forEach(bb::append);
		return bb.toBytes();
	}

	public static Bytes of(ByteBuffer bb) {
		var offset = bb.arrayOffset();
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

	public Bytes append(Bytes bytes1) {
		int size0 = size_(), size1 = bytes1.size_(), newSize = size0 + size1;
		var bsx = new byte[newSize];
		System.arraycopy(bs, start, bsx, 0, size0);
		System.arraycopy(bytes1.bs, bytes1.start, bsx, size0, size1);
		return of(bsx);
	}

	public <T> T collect(Fun<Bytes, T> fun) {
		return fun.apply(this);
	}

	public byte get(int index) {
		if (index < 0)
			index += size_();
		var i1 = index + start;
		checkClosedBounds(i1);
		return bs[i1];
	}

	public int indexOf(Bytes bytes, int start) {
		for (var i = start; i <= size_() - bytes.size_(); i++)
			if (startsWith(bytes, i))
				return i;
		return -1;
	}

	public boolean isEmpty() {
		return end <= start;
	}

	public boolean isZeroes() {
		var b = true;
		for (var i = start; b && i < end; i++)
			b &= bs[i] == 0;
		return b;
	}

	public Bytes pad(int size) {
		var bb = new BytesBuilder();
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
		var bb = new BytesBuilder();
		int i0 = 0, i;
		while (0 <= (i = indexOf(from, i0))) {
			bb.append(range_(i0, i));
			bb.append(to);
			i0 = i + from.size_();
		}
		bb.append(range_(i0));
		return bb.toBytes();
	}

	public int size() {
		return size_();
	}

	public boolean startsWith(Bytes bytes) {
		return startsWith_(bytes, 0);
	}

	public boolean startsWith(Bytes bytes, int s) {
		return startsWith_(bytes, s);
	}

	public byte[] toArray() {
		if (start != 0 || end != bs.length)
			return Arrays.copyOfRange(bs, start, end);
		else
			return bs;
	}

	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(bs, start, end - start);
	}

	public Bytes trim() {
		var s = start;
		var e = end;
		while (s < e && bs[s] == 0)
			s++;
		while (s < e && bs[e - 1] == 0)
			e--;
		return of(bs, s, e);
	}

	public void write(WriteByte out) {
		try {
			out.write(bs, start, end - start);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Override
	public Iterator<Byte> iterator() {
		return new Iterator<>() {
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
			var other = (Bytes) object;

			if (size_() == other.size_()) {
				var diff = other.start - start;
				for (var i = start; i < end; i++)
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
		var h = 7;
		for (var i = start; i < end; i++)
			h = h * 31 + bs[i];
		return h;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		for (var i = start; i < end; i++)
			sb.append(" " + To.hex2(bs[i]));
		return sb.toString();
	}

	private boolean startsWith_(Bytes bytes, int s) {
		if (s + bytes.size_() <= size_()) {
			var b = true;
			for (var i = 0; b && i < bytes.size_(); i++)
				b &= get(s + i) == bytes.get(i);
			return b;
		} else
			return false;
	}

	private Bytes range_(int s) {
		return range_(s, size_());
	}

	private Bytes range_(int s, int e) {
		var size = size_();
		if (s < 0)
			s += size;
		if (e < 0)
			e += size;
		s = min(size, s);
		e = min(size, e);
		int start_ = start + min(size, s);
		int end_ = start + min(size, e);
		var result = of(bs, start_, end_);

		// avoid small pack of bytes object keeping a large buffer
		if (Boolean.FALSE && reallocSize <= bs.length && end_ - start_ < reallocSize / 4)
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
			var inc = end - start;
			extendBuffer(size + inc);
			Bytes_.copy(bs_, start, bs, size, inc);
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
			var capacity0 = bs.length;

			if (capacity0 < capacity1) {
				int capacity = max(capacity0, 4);
				while (capacity < capacity1)
					capacity = capacity < Defaults.bufferSize ? capacity << 1 : capacity * 3 / 2;

				bs = Arrays.copyOf(bs, capacity);
			}
		}
	}

}
