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
			protected Bytes buffer = Bytes.empty;
			protected boolean isEof = false;

			public Bytes source() {
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);

				Bytes in;
				while (!isEof && bb.size() < bufferSize)
					if ((in = o.next()) != null)
						bb.append(in);
					else
						isEof = true;

				Bytes bytes = bb.toBytes();
				int n = Math.min(bytes.size(), bufferSize);
				Bytes head = bytes.subbytes(0, n);
				buffer = bytes.subbytes(n);

				return head;
			}
		});
	}

	public static Outlet<Bytes> concatSplit(Outlet<Bytes> o, Bytes delim) {
		int ds = delim.size();

		return new Outlet<>(new Source<Bytes>() {
			private Bytes buffer = Bytes.empty;
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
					for (int i = 0; isMatched_ && i < ds; i++)
						isMatched_ = buffer.get(p + i) == delim.get(i);
					if (isMatched_)
						isMatched = true;
					else
						p++;
				}

				return isMatched;
			}
		});
	}

	public static void copy(Outlet<Bytes> o, OutputStream os) {
		Bytes bytes;
		while ((bytes = o.next()) != null)
			try {
				bytes.write(os);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

}
