package suite.primitive;

import java.io.IOException;
import java.io.OutputStream;

import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class BytesUtil {

	private static final int bufferSize = 65536;

	private static abstract class FillingSource implements Source<Bytes> {
		private Source<Bytes> source;
		private int bufferSize;
		private Bytes buffer = Bytes.emptyBytes;
		private boolean isEof = false;

		public FillingSource(Source<Bytes> source, int bufferSize) {
			this.source = source;
			this.bufferSize = bufferSize;
		}

		public Bytes source() {
			BytesBuilder bb = new BytesBuilder();
			bb.append(buffer);

			Bytes bytes;
			while (!isEof && bb.size() < bufferSize)
				if ((bytes = source.source()) != null)
					bb.append(bytes);
				else
					isEof = true;

			Pair<Bytes, Bytes> pair = split(bb.toBytes(), isEof);
			buffer = pair.t1;
			return pair.t0;
		}

		protected abstract Pair<Bytes, Bytes> split(Bytes bytes, boolean isEof);
	}

	public static Source<Bytes> buffer(Source<Bytes> source) {
		return new FillingSource(source, bufferSize) {
			protected Pair<Bytes, Bytes> split(Bytes bytes, boolean isEof) {
				int n = Math.min(bytes.size(), bufferSize);
				return Pair.of(bytes.subbytes(0, n), bytes.subbytes(n));
			}
		};
	}

	public static void copy(Source<Bytes> source, OutputStream os) throws IOException {
		Bytes bytes;
		while ((bytes = source.source()) != null)
			bytes.write(os);
	}

	public static Source<Bytes> split(Source<Bytes> source, Bytes delim) {
		int ds = delim.size();

		return new FillingSource(source, bufferSize + ds) {
			protected Pair<Bytes, Bytes> split(Bytes bytes, boolean isEof) {
				if (!isEof || bytes.size() >= ds) {
					boolean isMatched = false;
					int p = 0;

					while (!isMatched && p + ds < bytes.size()) {
						boolean isMatched_ = true;
						for (int i = 0; i < ds; i++)
							if (bytes.get(p + i) != delim.get(i)) {
								isMatched_ = false;
								break;
							}
						if (isMatched_)
							isMatched = true;
						else
							p++;
					}

					Bytes head = bytes.subbytes(0, p);
					Bytes tail = bytes.subbytes(p + (isMatched ? ds : 0));
					return Pair.of(head, tail);
				} else
					return Pair.of(bytes, Bytes.emptyBytes);
			}
		};
	}

}
