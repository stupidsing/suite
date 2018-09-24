package suite.primitive;

import static suite.util.Friends.rethrow;

import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.Doubles.WriteChar;
import suite.primitive.streamlet.DblOutlet;
import suite.primitive.streamlet.DblStreamlet;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Read;

public class Doubles_ {

	private static int bufferSize = 65536;

	public static Outlet<Doubles> buffer(Outlet<Doubles> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static DblStreamlet concat(DblStreamlet... streamlets) {
		return new DblStreamlet(() -> {
			Source<DblStreamlet> source = Read.from(streamlets).outlet().source();
			return DblOutlet.of(DblFunUtil.concat(FunUtil.map(DblStreamlet::source, source)));
		});
	}

	public static double[] concat(double[]... array) {
		var length = 0;
		for (var fs : array)
			length += fs.length;
		var fs1 = new double[length];
		var i = 0;
		for (var fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Doubles concat(Doubles... array) {
		var length = 0;
		for (var doubles : array)
			length += doubles.size();
		var cs1 = new double[length];
		var i = 0;
		for (var doubles : array) {
			var size_ = doubles.size();
			copy(doubles.cs, doubles.start, cs1, i, size_);
			i += size_;
		}
		return Doubles.of(cs1);
	}

	public static void copy(double[] from, int fromIndex, double[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Doubles> outlet, WriteChar writer) {
		rethrow(() -> {
			Doubles doubles;
			while ((doubles = outlet.next()) != null)
				writer.write(doubles.cs, doubles.start, doubles.end - doubles.start);
			return doubles;
		});
	}

	public static DblStreamlet of(double... ts) {
		return new DblStreamlet(() -> DblOutlet.of(ts));
	}

	public static DblStreamlet of(double[] ts, int start, int end, int inc) {
		return new DblStreamlet(() -> DblOutlet.of(ts, start, end, inc));
	}

	public static DblStreamlet for_(double e) {
		return for_((double) 0, e);
	}

	public static DblStreamlet for_(double s, double e) {
		return new DblStreamlet(() -> {
			var m = DblMutable.of(s);
			return DblOutlet.of(() -> {
				var c = m.increment();
				return c < e ? c : DblFunUtil.EMPTYVALUE;
			});
		});
	}

	public static DblStreamlet reverse(double[] ts, int start, int end) {
		return new DblStreamlet(() -> DblOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Outlet<Doubles>> split(Doubles delim) {
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

	public static double[] toArray(int length, Int_Dbl f) {
		var cs = new double[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
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
			var cb = new DoublesBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toDoubles();
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
