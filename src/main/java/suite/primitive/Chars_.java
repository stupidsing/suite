package suite.primitive;

import java.io.IOException;

import suite.primitive.Chars.CharsBuilder;
import suite.primitive.Chars.WriteChar;
import suite.primitive.streamlet.ChrOutlet;
import suite.primitive.streamlet.ChrStreamlet;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Chars_ {

	private static int bufferSize = 65536;

	public static Outlet<Chars> buffer(Outlet<Chars> outlet) {
		return Outlet.of(new BufferedSource(outlet) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	@SafeVarargs
	public static ChrStreamlet concat(ChrStreamlet... streamlets) {
		return new ChrStreamlet(() -> {
			Source<ChrStreamlet> source = Read.from(streamlets).outlet().source();
			return ChrOutlet.of(ChrFunUtil.concat(FunUtil.map(ChrStreamlet::source, source)));
		});
	}

	public static char[] concat(char[]... array) {
		int length = 0;
		for (char[] fs : array)
			length += fs.length;
		char[] fs1 = new char[length];
		int i = 0;
		for (char[] fs : array) {
			int length_ = fs.length;
			copy(fs, 0, fs1, i, length_);
			i += length_;
		}
		return fs1;
	}

	public static Chars concat(Chars... array) {
		int length = 0;
		for (Chars chars : array)
			length += chars.size();
		char[] cs1 = new char[length];
		int i = 0;
		for (Chars chars : array) {
			int size_ = chars.size();
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

	public static void copy(Outlet<Chars> outlet, WriteChar writer) {
		Chars chars;
		while ((chars = outlet.next()) != null)
			try {
				writer.write(chars.cs, chars.start, chars.end - chars.start);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static ChrStreamlet of(char... ts) {
		return new ChrStreamlet(() -> ChrOutlet.of(ts));
	}

	public static ChrStreamlet of(char[] ts, int start, int end, int inc) {
		return new ChrStreamlet(() -> ChrOutlet.of(ts, start, end, inc));
	}

	public static ChrStreamlet range(char e) {
		return range((char) 0, e);
	}

	public static ChrStreamlet range(char s, char e) {
		return new ChrStreamlet(() -> {
			ChrMutable m = ChrMutable.of(s);
			return ChrOutlet.of(() -> {
				char c = m.increment();
				return c < e ? c : ChrFunUtil.EMPTYVALUE;
			});
		});
	}

	public static ChrStreamlet reverse(char[] ts, int start, int end) {
		return new ChrStreamlet(() -> ChrOutlet.of(ts, end - 1, start - 1, -1));
	}

	public static Fun<Outlet<Chars>, Outlet<Chars>> split(Chars delim) {
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

	public static char[] toArray(int length, Int_Chr f) {
		char[] cs = new char[length];
		for (int i = 0; i < length; i++)
			cs[i] = f.apply(i);
		return cs;
	}

	private static abstract class BufferedSource implements Source<Chars> {
		protected Outlet<Chars> outlet;
		protected Chars buffer = Chars.empty;
		protected boolean cont = true;
		protected int p0, p1;

		public BufferedSource(Outlet<Chars> outlet) {
			this.outlet = outlet;
		}

		public Chars source() {
			Chars in;
			CharsBuilder cb = new CharsBuilder();
			cb.append(buffer);

			p0 = 0;

			while (!search() && (cont &= (in = outlet.next()) != null)) {
				cb.append(in);
				buffer = cb.toChars();
			}

			if (cont && 0 < p0) {
				Chars head = buffer.range(0, p0);
				buffer = buffer.range(p1);
				return head;
			} else
				return null;
		}

		protected abstract boolean search(); // should set p0, p1
	}

}
