package suite.primitive;

import java.io.IOException;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.Longs.WriteChar;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Longs_ {

	private static int bufferSize = 65536;

	public static Outlet<Longs> buffer(Outlet<Longs> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static long[] concat(long[]... array) {
		int length = 0;
		for (long[] fs : array)
			length += fs.length;
		long[] fs1 = new long[length];
		int i = 0;
		for (long[] fs : array) {
			int length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Longs concat(Longs... array) {
		int length = 0;
		for (Longs longs : array)
			length += longs.size();
		long[] cs1 = new long[length];
		int i = 0;
		for (Longs longs : array) {
			int size_ = longs.size();
			copy(longs.cs, longs.start, cs1, i, size_);
			i += size_;
		}
		return Longs.of(cs1);
	}

	public static void copy(long[] from, int fromIndex, long[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Longs> outlet, WriteChar writer) {
		Longs longs;
		while ((longs = outlet.next()) != null)
			try {
				writer.write(longs.cs, longs.start, longs.end - longs.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static LngStreamlet range(long e) {
		return range((long) 0, e);
	}

	public static LngStreamlet range(long s, long e) {
		return new LngStreamlet(() -> {
			LngMutable m = LngMutable.of(s);
			return LngOutlet.of(() -> {
				long c = m.increment();
				return c < e ? c : LngFunUtil.EMPTYVALUE;
			});
		});
	}

	public static Fun<Outlet<Longs>, Outlet<Longs>> split(Longs delim) {
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

	public static long[] toArray(int length, Int_Lng f) {
		long[] cs = new long[length];
		for (int i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Longs> {
		protected Outlet<Longs> outlet;
		protected Longs buffer = Longs.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Longs> outlet) {
			this.outlet = outlet;
		}

		public Longs source() {
			Longs in;
			LongsBuilder cb = new LongsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toLongs();
			}

			if (cont && 0 < p0) {
				Longs head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
