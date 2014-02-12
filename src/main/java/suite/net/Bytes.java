package suite.net;

import java.util.Arrays;
import java.util.Comparator;

import suite.util.Copy;
import suite.util.Util;

public class Bytes {

	private byte bytes[]; // Immutable
	private int start, end;

	private static final byte emptyByteArray[] = new byte[0];

	private static final int reallocSize = 65536;

	public static final Bytes emptyBytes = new Bytes(emptyByteArray);

	public static final Comparator<Bytes> comparator = new Comparator<Bytes>() {
		public int compare(Bytes bytes0, Bytes bytes1) {
			int start0 = bytes0.start, start1 = bytes1.start;
			int size0 = bytes0.size(), size1 = bytes1.size(), minSize = Math.min(size0, size1);
			int index = 0, c = 0;

			while (c == 0 && index < minSize) {
				byte b0 = bytes0.bytes[start0 + index];
				byte b1 = bytes1.bytes[start1 + index];
				c = b0 == b1 ? 0 : b0 > b1 ? 1 : -1;
				index++;
			}

			return c != 0 ? c : size0 - size1;
		}
	};

	public Bytes(byte bytes[]) {
		this(bytes, 0);
	}

	public Bytes(byte bytes[], int start) {
		this(bytes, start, bytes.length);
	}

	public Bytes(Bytes bytes) {
		this(bytes.bytes, bytes.start, bytes.end);
	}

	public Bytes(byte bytes[], int start, int end) {
		this.bytes = bytes;
		this.start = start;
		this.end = end;
	}

	public Bytes append(Bytes a) {
		int size0 = size(), size1 = a.size(), newSize = size0 + size1;
		byte nb[] = new byte[newSize];
		System.arraycopy(bytes, start, nb, 0, size0);
		System.arraycopy(a.bytes, a.start, nb, size0, size1);
		return new Bytes(nb);
	}

	public byte get(int index) {
		int i1 = index + start;
		checkClosedBounds(i1);
		return bytes[i1];
	}

	public boolean isEmpty() {
		return start >= end;
	}

	public int size() {
		return end - start;
	}

	public Bytes subbytes(int start) {
		return subbytes0(this.start + start, end);
	}

	public Bytes subbytes(int start, int end) {
		return subbytes0(this.start + start, this.start + end);
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++)
			result = 31 * result + bytes[i];
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Bytes.class) {
			Bytes other = (Bytes) object;
			int diff = other.start - start;

			for (int i = start; i < end; i++)
				if (bytes[i] != other.bytes[i + diff])
					return false;

			return true;
		} else
			return false;
	}

	private Bytes subbytes0(int start, int end) {
		checkOpenBounds(start);
		checkOpenBounds(end);
		Bytes result = new Bytes(bytes, start, end);

		// Avoid small pack of bytes object keeping a large buffer
		if (bytes.length >= reallocSize && end - start < reallocSize / 4)
			result = emptyBytes.append(result); // Do not share reference

		return result;
	}

	private void checkOpenBounds(int index) {
		if (index < start || index > end)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "}");
	}

	private void checkClosedBounds(int index) {
		if (index < start || index >= end)
			throw new IndexOutOfBoundsException("Index " + (index - start) + " is not within [0-" + (end - start) + "]");
	}

	private static final String hexDigits = "0123456789ABCDEF";

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(" ") //
					.append(hexDigits.charAt(bytes[i] >>> 4 & 0x0F)) //
					.append(hexDigits.charAt(bytes[i] & 0x0F));
		return sb.toString();
	}

	public byte[] getBytes() {
		if (start != 0 || end != bytes.length)
			return Arrays.copyOfRange(bytes, start, end);
		else
			return bytes;
	}

	public static class BytesBuilder {
		private byte bytes[] = emptyByteArray;
		private int size;

		public BytesBuilder() {
		}

		public BytesBuilder append(Bytes b) {
			return append(b.bytes, b.start, b.end);
		}

		public BytesBuilder append(byte b) {
			return append(new byte[] { b });
		}

		public BytesBuilder append(byte b[]) {
			return append(b, 0, b.length);
		}

		public BytesBuilder append(byte b[], int start, int end) {
			int inc = end - start, size1 = size + inc;

			if (bytes.length < size1)
				extendBuffer(size1);

			Copy.primitiveArray(b, start, bytes, size, inc);

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
			return new Bytes(bytes, 0, size);
		}

		private void extendBuffer(int size1) {
			int length1 = bytes.length != 0 ? bytes.length : 1;
			while (length1 < size1)
				length1 = length1 < 4096 ? length1 << 1 : length1 * 3 / 2;

			byte bytes1[] = new byte[length1];
			Copy.primitiveArray(bytes, 0, bytes1, 0, size);
			bytes = bytes1;
		}
	}

}
