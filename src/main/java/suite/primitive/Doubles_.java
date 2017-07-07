package suite.primitive;

import java.io.IOException;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.Doubles.WriteChar;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Doubles_ {

	private static int bufferSize = 65536;

	public static Outlet<Doubles> buffer(Outlet<Doubles> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(double[] from, int fromIndex, double[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Doubles> outlet, WriteChar writer) {
		Doubles doubles;
		while ((doubles = outlet.next()) != null)
			try {
				writer.write(doubles.cs, doubles.start, doubles.end - doubles.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Doubles>, Outlet<Doubles>> split(Doubles delim) {
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

	private static abstract class BufferedSource implements Source<Doubles> {
		protected Outlet<Doubles> outlet;
		protected Doubles buffer = Doubles.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Doubles> outlet) {
			this.outlet = outlet;
		}

		public Doubles source() {
			Doubles in;
			DoublesBuilder cb = new DoublesBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toDoubles();
			}

			if (cont && 0 < p0) {
				Doubles head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
