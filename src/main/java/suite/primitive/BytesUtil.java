package suite.primitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;

public class BytesUtil {

	private static final int bufferSize = 65536;

	public static Source<Bytes> buffer(Source<Bytes> source) {
		return new Source<Bytes>() {
			private Bytes buffer = Bytes.emptyBytes;

			public Bytes source() {
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);
				fill(bb, source);

				if (bb.size() > 0) {
					Bytes bytes = bb.toBytes();
					int n = Math.min(bytes.size(), bufferSize);
					Bytes head = bytes.subbytes(0, n);
					buffer = bytes.subbytes(n);
					return head;
				} else
					return null;
			}
		};
	}

	public static void sink(Source<Bytes> source, OutputStream os) throws IOException {
		Bytes bytes;
		while ((bytes = source.source()) != null)
			bytes.write(os);
	}

	public static Source<Bytes> source(InputStream is) {
		return () -> {
			byte bs[] = new byte[bufferSize];
			int nBytesRead;
			try {
				nBytesRead = is.read(bs);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			if (nBytesRead >= 0)
				return Bytes.of(bs, 0, nBytesRead);
			else {
				try {
					is.close();
				} catch (IOException ex) {
					LogUtil.error(ex);
				}
				return null;
			}
		};
	}

	public static Source<Bytes> split(Source<Bytes> source, byte delim) {
		return new Source<Bytes>() {
			private Bytes buffer = Bytes.emptyBytes;

			public Bytes source() {
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);
				fill(bb, source);

				if (bb.size() > 0) {
					Bytes bytes = bb.toBytes();

					int n = 0;
					for (; n < bytes.size(); n++)
						if (bytes.get(n) == delim)
							break;

					Bytes head = bytes.subbytes(0, n);
					buffer = bytes.subbytes(n);
					return head;
				} else
					return null;
			}
		};
	}

	private static void fill(BytesBuilder bb, Source<Bytes> source) {
		Bytes bytes;
		while (bb.size() < bufferSize && (bytes = source.source()) != null)
			bb.append(bytes);
	}

}
