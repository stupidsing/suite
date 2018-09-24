package suite.primitive;

import static suite.util.Friends.rethrow;

import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Floats.WriteChar;
import suite.primitive.streamlet.FltOutlet;
import suite.primitive.streamlet.FltStreamlet;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Read;

public class Floats_ {

	private static int bufferSize = 65536;

	public static Outlet<Floats> buffer(Outlet<Floats> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static FltStreamlet concat(FltStreamlet... streamlets) {
		return new FltStreamlet(() -> {
			var source = Read.from(streamlets).outlet().source();
			return FltOutlet.of(FltFunUtil.concat(FunUtil.map(FltStreamlet::source, source)));
		});
	}

	public static float[] concat(float[]... array) {
		var length = 0;
		for (var fs : array)
			length += fs.length;
		var fs1 = new float[length];
		var i = 0;
		for (var fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Floats concat(Floats... array) {
		var length = 0;
		for (var floats : array)
			length += floats.size();
		var cs1 = new float[length];
		var i = 0;
		for (var floats : array) {
			var size_ = floats.size();
			copy(floats.cs, floats.start, cs1, i, size_);
			i += size_;
		}
		return Floats.of(cs1);
	}

	public static void copy(float[] from, int fromIndex, float[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Floats> outlet, WriteChar writer) {
		rethrow(() -> {
			Floats floats;
			while ((floats = outlet.next()) != null)
				writer.write(floats.cs, floats.start, floats.end - floats.start);
			return floats;
		});
	}

	public static FltStreamlet of(float... ts) {
		return new FltStreamlet(() -> FltOutlet.of(ts));
	}

	public static FltStreamlet of(float[] ts, int start, int end, int inc) {
		return new FltStreamlet(() -> FltOutlet.of(ts, start, end, inc));
	}

	public static FltStreamlet for_(float e) {
		return for_((float) 0, e);
	}

	public static FltStreamlet for_(float s, float e) {
		return new FltStreamlet(() -> {
			var m = FltMutable.of(s);
			return FltOutlet.of(() -> {
				var c = m.increment();
				return c < e ? c : FltFunUtil.EMPTYVALUE;
			});
		});
	}

	public static FltStreamlet reverse(float[] ts, int start, int end) {
		return new FltStreamlet(() -> FltOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Outlet<Floats>> split(Floats delim) {
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

	public static float[] toArray(int length, Int_Flt f) {
		var cs = new float[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
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
			var cb = new FloatsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toFloats();
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
