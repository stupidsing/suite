package suite.primitive;

import static suite.util.Friends.rethrow;

import suite.primitive.Longs.LongsBuilder;
import suite.primitive.Longs.WriteChar;
import suite.primitive.streamlet.LngOutlet;
import suite.primitive.streamlet.LngStreamlet;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Read;

public class Longs_ {

	private static int bufferSize = 65536;

	public static Outlet<Longs> buffer(Outlet<Longs> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static LngStreamlet concat(LngStreamlet... streamlets) {
		return new LngStreamlet(() -> {
			Source<LngStreamlet> source = Read.from(streamlets).outlet().source();
			return LngOutlet.of(LngFunUtil.concat(FunUtil.map(LngStreamlet::source, source)));
		});
	}

	public static long[] concat(long[]... array) {
		var length = 0;
		for (var fs : array)
			length += fs.length;
		var fs1 = new long[length];
		var i = 0;
		for (var fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Longs concat(Longs... array) {
		var length = 0;
		for (var longs : array)
			length += longs.size();
		var cs1 = new long[length];
		var i = 0;
		for (var longs : array) {
			var size_ = longs.size();
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
		rethrow(() -> {
			Longs longs;
			while ((longs = outlet.next()) != null)
				writer.write(longs.cs, longs.start, longs.end - longs.start);
			return longs;
		});
	}

	public static LngStreamlet of(long... ts) {
		return new LngStreamlet(() -> LngOutlet.of(ts));
	}

	public static LngStreamlet of(long[] ts, int start, int end, int inc) {
		return new LngStreamlet(() -> LngOutlet.of(ts, start, end, inc));
	}

	public static LngStreamlet for_(long e) {
		return for_((long) 0, e);
	}

	public static LngStreamlet for_(long s, long e) {
		return new LngStreamlet(() -> {
			var m = LngMutable.of(s);
			return LngOutlet.of(() -> {
				var c = m.increment();
				return c < e ? c : LngFunUtil.EMPTYVALUE;
			});
		});
	}

	public static LngStreamlet reverse(long[] ts, int start, int end) {
		return new LngStreamlet(() -> LngOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Outlet<Longs>> split(Longs delim) {
		var ds = delim.size();

		return outlet -> Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				var size = buffer.size();
				while ((p1 = p0 + ds) <= size)
					if (!delim.equals(buffer.range(p0, p1)))
						p0++;
					else
						return true;
				var b = !cont;
				if (b)
					p0 = p1 = buffer.size();
				return b;
			}
		});
	}

	public static long[] toArray(int length, Int_Lng f) {
		var cs = new long[length];
		for (var i = 0; i < length; i++)
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
			var cb = new LongsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toLongs();
			}

			if (cont && 0 < p0) {
				var head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
