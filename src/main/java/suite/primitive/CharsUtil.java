package suite.primitive;

import java.io.IOException;
import java.io.Writer;

import suite.primitive.Chars.CharsBuilder;
import suite.util.FunUtil.Source;

public class CharsUtil {

	private static final int bufferSize = 65536;

	public static Source<Chars> buffer(Source<Chars> source) {
		return new Source<Chars>() {
			private Source<Chars> source_ = source;
			protected Chars buffer = Chars.emptyChars;
			protected boolean isEof = false;

			public Chars source() {
				fill();
				int n = Math.min(buffer.size(), bufferSize);
				Chars head = buffer.subchars(0, n);
				buffer = buffer.subchars(n);
				return head;
			}

			protected void fill() {
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				Chars chars;
				while (!isEof && cb.size() < bufferSize)
					if ((chars = source_.source()) != null)
						cb.append(chars);
					else
						isEof = true;
				buffer = cb.toChars();
			}
		};
	}

	public static Source<Chars> concatSplit(Source<Chars> source, Chars delim) {
		int ds = delim.size();

		return new Source<Chars>() {
			private Chars buffer = Chars.emptyChars;
			private boolean isArriving;
			private int p;

			public Chars source() {
				Chars chars;
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				p = 0;

				while (!search(delim) && (isArriving = (chars = source.source()) != null)) {
					cb.append(chars);
					buffer = cb.toChars();
				}

				if (isArriving) {
					Chars head = buffer.subchars(0, p);
					buffer = buffer.subchars(p + ds);
					return head;
				} else
					return !buffer.isEmpty() ? buffer : null;
			}

			private boolean search(Chars delim) {
				boolean isMatched = false;

				while (!isMatched && p + ds <= buffer.size()) {
					boolean isMatched_ = true;
					for (int i = 0; i < ds; i++)
						if (buffer.get(p + i) != delim.get(i)) {
							isMatched_ = false;
							break;
						}
					if (isMatched_)
						isMatched = true;
					else
						p++;
				}

				return isMatched;
			}
		};
	}

	public static void copy(Source<Chars> source, Writer writer) throws IOException {
		Chars chars;
		while ((chars = source.source()) != null)
			chars.write(writer);
	}

}
