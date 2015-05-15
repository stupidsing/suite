package suite.primitive;

import java.io.IOException;
import java.io.OutputStream;

import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;

public class BytesUtil {

	private static final int bufferSize = 65536;

	public static Outlet<Bytes> buffer(Outlet<Bytes> o) {
		return new Outlet<>(new Source<Bytes>() {
			private Outlet<Bytes> o_ = o;
			protected Bytes buffer = Bytes.emptyBytes;
			protected boolean isEof = false;

			public Bytes source() {
				fill();
				int n = Math.min(buffer.size(), bufferSize);
				Bytes head = buffer.subbytes(0, n);
				buffer = buffer.subbytes(n);
				return head;
			}

			private void fill() {
				BytesBuilder cb = new BytesBuilder();
				cb.append(buffer);

				Bytes Bytes;
				while (!isEof && cb.size() < bufferSize)
					if ((Bytes = o_.next()) != null)
						cb.append(Bytes);
					else
						isEof = true;
				buffer = cb.toBytes();
			}
		});
	}

	public static Outlet<Bytes> concatSplit(Outlet<Bytes> o, Bytes delim) {
		int ds = delim.size();

		return new Outlet<>(new Source<Bytes>() {
			private Bytes buffer = Bytes.emptyBytes;
			private boolean isArriving;
			private int p;

			public Bytes source() {
				Bytes bytes;
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);

				p = 0;

				while (isArriving && !search(delim) && (isArriving = (bytes = o.next()) != null)) {
					bb.append(bytes);
					buffer = bb.toBytes();
				}

				if (isArriving) {
					Bytes head = buffer.subbytes(0, p);
					buffer = buffer.subbytes(p + ds);
					return head;
				} else
					return !buffer.isEmpty() ? buffer : null;
			}

			private boolean search(Bytes delim) {
				boolean isMatched = false;

				while (!isMatched && p + ds <= buffer.size()) {
					boolean isMatched_ = true;
					for (int i = 0; i < ds; i++)
						if (buffer.get(p + i) != delim.get(i)) {
							isMatched_ = false;
							break;
						}
					if (isMatched_)
						isMatched = true;
					else
						p++;
				}

				return isMatched;
			}
		});
	}

	public static void copy(Outlet<Bytes> o, OutputStream os) throws IOException {
		Bytes bytes;
		while ((bytes = o.next()) != null)
			bytes.write(os);
	}

	public static boolean isZeroes(Bytes bytes) {
		boolean result = true;
		for (int i = bytes.start; result && i < bytes.end; i++)
			result &= bytes.bs[i] == 0;
		return result;
	}

	public static Bytes trim(Bytes bytes) {
		byte bs[] = bytes.bs;
		int start = bytes.start;
		int end = bytes.end;
		while (start < end && bs[start] == 0)
			start++;
		while (start < end && bs[end - 1] == 0)
			end--;
		return Bytes.of(bs, start, end);
	}

}
