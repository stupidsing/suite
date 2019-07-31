package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.Chars.WriteChar;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.FunUtil;
import suite.streamlet.Puller;
import suite.streamlet.Read;

public class Chars_ {

	private static int bufferSize = 65536;

	public static Puller<Chars> buffer(Puller<Chars> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static ChrStreamlet concat(ChrStreamlet... streamlets) {
		return new ChrStreamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return ChrPuller.of(ChrFunUtil.concat(FunUtil.map(ChrStreamlet::source, source)));
		});
	}

	public static char[] concat(char[]... array) {
		var length = 0;
		for (var fs : array)
			length += fs.length;
		var fs1 = new char[length];
		var i = 0;
		for (var fs : array) {
			var length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Chars concat(Chars... array) {
		var length = 0;
		for (var chars : array)
			length += chars.size();
		var cs1 = new char[length];
		var i = 0;
		for (var chars : array) {
			var size_ = chars.size();
			copy(chars.cs, chars.start, cs1, i, size_);
			i += size_;
		}
		return Chars.of(cs1);
	}

	public static void copy(char[] from, int fromIndex, char[] to, int toIndex, int size) {
		if (0 < size)
			System.arraycopy(from, fromIndex, to, toIndex, size);
		else if (size < 0)
			throw new IndexOutOfBoundsException();
	}

	public static void copy(Puller<Chars> puller, WriteChar writer) {
		ex(() -> {
			Chars chars;
			while ((chars = puller.pull()) != null)
				writer.write(chars.cs, chars.start, chars.end - chars.start);
			return chars;
		});
	}

	public static ChrStreamlet of(char... ts) {
		return new ChrStreamlet(() -> ChrPuller.of(ts));
	}

	public static ChrStreamlet of(char[] ts, int start, int end, int inc) {
		return new ChrStreamlet(() -> ChrPuller.of(ts, start, end, inc));
	}

	public static ChrStreamlet for_(char s, char e) {
		return new ChrStreamlet(() -> {
			var m = ChrMutable.of(s);
			return ChrPuller.of(() -> {
				var c = m.increment();
				return c < e ? c : ChrFunUtil.EMPTYVALUE;
			});
		});
	}

	public static ChrStreamlet reverse(char[] ts, int start, int end) {
		return new ChrStreamlet(() -> ChrPuller.of(ts, end - 1, start - 1, -1));
	}

	public static Iterate<Puller<Chars>> split(Chars delim) {
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

	public static char[] toArray(int length, Int_Chr f) {
		var cs = new char[length];
		for (var i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Chars> {
		protected Puller<Chars> puller;
		protected Chars buffer = Chars.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Puller<Chars> puller) {
			this.puller = puller;
		}

		public Chars g() {
			Chars in;
			var cb = new CharsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = puller.pull()) != null)) {
				cb.append(in);
				buffer = cb.toChars();
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
