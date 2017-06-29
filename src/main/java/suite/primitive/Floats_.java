package suite.primitive;

import java.io.IOException;

import suite.primitive.Floats.FltsBuilder;
import suite.primitive.Floats.WriteFloat;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Floats_ {

	private static int bufferSize = 65536;

	public static Outlet<Floats> buffer(Outlet<Floats> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Outlet<Floats> outlet, WriteFloat writer) {
		Floats floats;
		while ((floats = outlet.next()) != null)
			try {
				writer.write(floats.cs, floats.start, floats.end - floats.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Floats>, Outlet<Floats>> split(Floats delim) {
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

	private static abstract class BufferedSource implements Source<Floats> {
		protected Outlet<Floats> outlet;
		protected Floats buffer = Floats.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Floats> outlet) {
			this.outlet = outlet;
		}

		public Floats source() {
			Floats in;
			FltsBuilder cb = new FltsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toFloats();
			}

			if (cont && 0 < p0) {
				Floats head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
