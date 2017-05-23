package suite.primitive;

import java.io.IOException;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.Ints.WriteInt;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Ints_ {

	private static int bufferSize = 65536;

	public static Outlet<Ints> buffer(Outlet<Ints> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Outlet<Ints> outlet, WriteInt writer) {
		Ints ints;
		while ((ints = outlet.next()) != null)
			try {
				writer.write(ints.cs, ints.start, ints.end - ints.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Ints>, Outlet<Ints>> split(Ints delim) {
		int ds = delim.size();

		return outlet -> Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				int size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
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

	private static abstract class BufferedSource implements Source<Ints> {
		protected Outlet<Ints> outlet;
		protected Ints buffer = Ints.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Ints> outlet) {
			this.outlet = outlet;
		}

		public Ints source() {
			Ints in;
			IntsBuilder cb = new IntsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toInts();
			}

			if (cont && 0 < p0) {
				Ints head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
