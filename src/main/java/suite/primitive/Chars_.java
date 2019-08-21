package suite.primitive;

import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Source;
import primal.primitive.adt.Chars;
import primal.primitive.adt.Chars.CharsBuilder;
import primal.primitive.adt.Chars.WriteChar;
import primal.primitive.puller.ChrPuller;
import primal.primitive.streamlet.ChrStreamlet;
import primal.puller.Puller;

public class Chars_ {

	private static int bufferSize = 65536;

	public static Puller<Chars> buffer(Puller<Chars> puller) {
		return Puller.of(new BufferedSource(puller) {
			protected boolean search() {
				return bufferSize <= (p0 = p1 = buffer.size());
			}
		});
	}

	public static void copy(Puller<Chars> puller, WriteChar writer) {
		ex(() -> {
			Chars chars;
			while ((chars = puller.pull()) != null)
				writer.write(chars.cs, chars.start, chars.end - chars.start);
			return chars;
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
