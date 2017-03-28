package suite.primitive;

import java.io.IOException;
import java.io.Writer;

import suite.primitive.Chars.CharsBuilder;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class CharsUtil {

	private static final int bufferSize = 65536;

	public static Outlet<Chars> buffer(Outlet<Chars> o) {
		return new Outlet<>(new Source<Chars>() {
			protected Chars buffer = Chars.empty;
			private boolean cont = true;

			public Chars source() {
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				Chars in;
				while (cb.size() < bufferSize && (cont &= (in = o.next()) != null))
					cb.append(in);

				if (cont || 0 < buffer.size()) {
					Chars chars = cb.toChars();
					int n = Math.min(chars.size(), bufferSize);
					Chars head = chars.subchars(0, n);
					buffer = chars.subchars(n);
					return head;
				} else
					return null;
			}
		});
	}

	public static void copy(Outlet<Chars> o, Writer writer) {
		Chars chars;
		while ((chars = o.next()) != null)
			try {
				chars.write(writer);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
	}

	public static Fun<Outlet<Chars>, Outlet<Chars>> split(Chars delim) {
		int ds = delim.size();

		return o -> new Outlet<>(new Source<Chars>() {
			private Chars buffer = Chars.empty;
			private boolean cont = true;
			private int p;

			public Chars source() {
				Chars chars;
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				p = 0;

				while (!search(delim) && (cont &= (chars = o.next()) != null)) {
					cb.append(chars);
					buffer = cb.toChars();
				}

				if (cont || 0 < buffer.size()) {
					p = 0 < p ? p : buffer.size();
					Chars head = buffer.subchars(0, p);
					buffer = buffer.subchars(p + ds);
					return head;
				} else
					return null;
			}

			private boolean search(Chars delim) {
				boolean isMatched = false;

				while (!isMatched && p <= buffer.size()) {
					boolean isMatched_ = true;
					for (int i = 0; isMatched_ && i < ds; i++)
						if (p + i <= buffer.size())
							isMatched_ = buffer.get(p + i) == delim.get(i);
					if (isMatched_)
						isMatched = true;
					else
						p++;
				}

				return isMatched;
			}
		});
	}

}
