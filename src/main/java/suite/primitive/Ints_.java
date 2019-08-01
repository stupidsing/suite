package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.FunUtil;
import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.IntPrim;
import primal.primitive.fp.IntFunUtil;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.Ints.WriteChar;
import suite.primitive.streamlet.IntPuller;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Puller;
import suite.streamlet.Read;

public class Ints_ {

	private static int bufferSize = 65536;

	public static Puller<Ints> buffer(Puller<Ints> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static IntStreamlet concat(IntStreamlet... streamlets) {
		return new IntStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return IntPuller.of(IntFunUtil.concat(FunUtil.map(IntStreamlet::source, source)));
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

	public static void copy(Puller<Ints> puller, WriteChar writer) {
		ex(() -> {
			Ints ints;
			while ((ints = puller.pull()) != null)
				writer.write(ints.cs, ints.start, ints.end - ints.start);
			return ints;
		});
	}

	public static IntStreamlet of(int... ts) {
		return new IntStreamlet(() -> IntPuller.of(ts));
	}

	public static IntStreamlet of(int[] ts, int start, int end, int inc) {
		return new IntStreamlet(() -> IntPuller.of(ts, start, end, inc));
	}

	public static IntStreamlet for_(int s, int e) {
		return new IntStreamlet(() -> {
			var m = IntMutable.of(s);
			return IntPuller.of(() -> {
				var c = m.increment();
				return c < e ? c : IntPrim.EMPTYVALUE;
			});
		});
	}

	public static IntStreamlet reverse(int[] ts, int start, int end) {
		return new IntStreamlet(() -> IntPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Ints>> split(Ints delim) {
		var ds = delim.size();

		return puller -> Puller.of(new BufferedSource(puller) {
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
		protected Puller<Ints> puller;
		protected Ints buffer = Ints.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Ints> puller) {
			this.puller = puller;
		}

		public Ints g() {
			Ints in;
			var cb = new IntsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
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
