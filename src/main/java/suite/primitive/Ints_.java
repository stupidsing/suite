package suite.primitive;

import static suite.util.Friends.rethrow;

import suite.primitive.Ints.IntsBuilder;
import suite.primitive.Ints.WriteChar;
import suite.primitive.streamlet.IntOutlet;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Read;

public class Ints_ {

	private static int bufferSize = 65536;

	public static Outlet<Ints> buffer(Outlet<Ints> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static IntStreamlet concat(IntStreamlet... streamlets) {
		return new IntStreamlet(() -> {
			Source<IntStreamlet> source = Read.from(streamlets).outlet().source();
			return IntOutlet.of(IntFunUtil.concat(FunUtil.map(IntStreamlet::source, source)));
		});
	}

	public static int[] concat(int[]... array) {
		var length = 0;
		for (var fs : array)
			length += fs.length;
		var fs1 = new int[length];
		var i = 0;
		for (var fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Ints concat(Ints... array) {
		var length = 0;
		for (var ints : array)
			length += ints.size();
		var cs1 = new int[length];
		var i = 0;
		for (var ints : array) {
			var size_ = ints.size();
			copy(ints.cs, ints.start, cs1, i, size_);
			i += size_;
		}
		return Ints.of(cs1);
	}

	public static void copy(int[] from, int fromIndex, int[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Outlet<Ints> outlet, WriteChar writer) {
		rethrow(() -> {
			Ints ints;
			while ((ints = outlet.next()) != null)
				writer.write(ints.cs, ints.start, ints.end - ints.start);
			return ints;
		});
	}

	public static IntStreamlet of(int... ts) {
		return new IntStreamlet(() -> IntOutlet.of(ts));
	}

	public static IntStreamlet of(int[] ts, int start, int end, int inc) {
		return new IntStreamlet(() -> IntOutlet.of(ts, start, end, inc));
	}

	public static IntStreamlet for_(int e) {
		return for_((int) 0, e);
	}

	public static IntStreamlet for_(int s, int e) {
		return new IntStreamlet(() -> {
			var m = IntMutable.of(s);
			return IntOutlet.of(() -> {
				var c = m.increment();
				return c < e ? c : IntFunUtil.EMPTYVALUE;
			});
		});
	}

	public static IntStreamlet reverse(int[] ts, int start, int end) {
		return new IntStreamlet(() -> IntOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Outlet<Ints>> split(Ints delim) {
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

	public static int[] toArray(int length, Int_Int f) {
		var cs = new int[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
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
			var cb = new IntsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toInts();
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
