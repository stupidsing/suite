package suite.primitive;

import java.io.IOException;

import suite.primitive.Shorts.ShortsBuilder;
import suite.primitive.Shorts.WriteShort;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Shorts_ {

	private static int bufferSize = 65536;

	public static Outlet<Shorts> buffer(Outlet<Shorts> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(short[] from, int fromIndex, short[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Shorts> outlet, WriteShort writer) {
		Shorts shorts;
		while ((shorts = outlet.next()) != null)
			try {
				writer.write(shorts.cs, shorts.start, shorts.end - shorts.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Shorts>, Outlet<Shorts>> split(Shorts delim) {
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

	private static abstract class BufferedSource implements Source<Shorts> {
		protected Outlet<Shorts> outlet;
		protected Shorts buffer = Shorts.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Shorts> outlet) {
			this.outlet = outlet;
		}

		public Shorts source() {
			Shorts in;
			ShortsBuilder cb = new ShortsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toShorts();
			}

			if (cont && 0 < p0) {
				Shorts head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
