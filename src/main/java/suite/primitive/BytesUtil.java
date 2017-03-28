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
		return Outlet.from(new BufferedSource(o) {
			protected boolean search() {
				int size = buffer.size();
				if (size < bufferSize)
					return false;
				else {
					p0 = p1 = size;
					return true;
				}
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

		return o -> Outlet.from(new BufferedSource(o) {
			protected boolean search() {
				int size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.subbytes(p0, p1)))
						p0++;
					else
						return true;
				if (!cont) {
					p0 = p1 = buffer.size();
					return true;
				} else
					return false;
			}
		});
	}

	public static abstract class BufferedSource implements Source<Bytes> {
		protected Outlet<Bytes> outlet;
		protected Bytes buffer = Bytes.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Bytes> outlet) {
			this.outlet = outlet;
		}

		public Bytes source() {
			Bytes in;
			BytesBuilder bb = new BytesBuilder();
			bb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				bb.append(in);
				buffer = bb.toBytes();
			}

			if (cont && 0 < p0) {
				Bytes head = buffer.subbytes(0, p0);
				buffer = buffer.subbytes(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search();
	}

}
