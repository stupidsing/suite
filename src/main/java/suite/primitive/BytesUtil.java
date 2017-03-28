package suite.primitive;

import java.io.IOException;
import java.io.OutputStream;

import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class BytesUtil {

	private static final int bufferSize = 65536;

	public static Outlet<Bytes> buffer(Outlet<Bytes> o) {
		return Outlet.from(new Source<Bytes>() {
			protected Bytes buffer = Bytes.empty;
			protected boolean cont = true;

			public Bytes source() {
				Bytes in;
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);

				while (bb.size() < bufferSize && (cont &= (in = o.next()) != null))
					bb.append(in);

				if (cont || 0 < buffer.size()) {
					Bytes bytes = bb.toBytes();
					int n = Math.min(bytes.size(), bufferSize);
					Bytes head = bytes.subbytes(0, n);
					buffer = bytes.subbytes(n);
					return head;
				} else
					return null;
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

	public static Fun<Outlet<Bytes>, Outlet<Bytes>> split(Bytes delim) {
		int ds = delim.size();

		return o -> Outlet.from(new Source<Bytes>() {
			private Bytes buffer = Bytes.empty;
			private boolean cont = true;
			private int p;

			public Bytes source() {
				Bytes in;
				BytesBuilder bb = new BytesBuilder();
				bb.append(buffer);

				p = 0;

				while (!search(delim) && (cont &= (in = o.next()) != null)) {
					bb.append(in);
					buffer = bb.toBytes();
				}

				if (cont || 0 < buffer.size()) {
					p = 0 < p ? p : buffer.size();
					Bytes head = buffer.subbytes(0, p);
					buffer = buffer.subbytes(p + ds);
					return head;
				} else
					return null;
			}

			private boolean search(Bytes delim) {
				boolean isMatched = false;

				while (!isMatched && p < buffer.size()) {
					boolean isMatched_ = p + ds <= buffer.size();
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

}
