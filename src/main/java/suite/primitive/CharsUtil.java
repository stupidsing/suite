package suite.primitive;

import java.io.IOException;
import java.io.Writer;

import suite.primitive.Chars.CharsBuilder;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class CharsUtil {

	private static final int bufferSize = 65536;

	private static abstract class FillingSource implements Source<Chars> {
		private Source<Chars> source;
		private int bufferSize;
		private Chars buffer = Chars.emptyChars;
		private boolean isEof = false;

		public FillingSource(Source<Chars> source, int bufferSize) {
			this.source = source;
			this.bufferSize = bufferSize;
		}

		public Chars source() {
			CharsBuilder cb = new CharsBuilder();
			cb.append(buffer);

			Chars chars;
			while (!isEof && cb.size() < bufferSize)
				if ((chars = source.source()) != null)
					cb.append(chars);
				else
					isEof = true;

			Pair<Chars, Chars> pair = split(cb.toChars(), isEof);
			buffer = pair.t1;
			return pair.t0;
		}

		protected abstract Pair<Chars, Chars> split(Chars chars, boolean isEof);
	}

	public static Source<Chars> buffer(Source<Chars> source) {
		return new FillingSource(source, bufferSize) {
			protected Pair<Chars, Chars> split(Chars chars, boolean isEof) {
				int n = Math.min(chars.size(), bufferSize);
				return Pair.of(chars.subchars(0, n), chars.subchars(n));
			}
		};
	}

	public static void copy(Source<Chars> source, Writer writer) throws IOException {
		Chars chars;
		while ((chars = source.source()) != null)
			chars.write(writer);
	}

	public static Source<Chars> split(Source<Chars> source, Chars delim) {
		int ds = delim.size();

		return new FillingSource(source, bufferSize + ds) {
			protected Pair<Chars, Chars> split(Chars chars, boolean isEof) {
				if (!isEof || chars.size() >= ds) {
					boolean isMatched = false;
					int p = 0;

					while (!isMatched && p + ds < chars.size()) {
						boolean isMatched_ = true;
						for (int i = 0; i < ds; i++)
							if (chars.get(p + i) != delim.get(i)) {
								isMatched_ = false;
								break;
							}
						if (isMatched_)
							isMatched = true;
						else
							p++;
					}

					Chars head = chars.subchars(0, p);
					Chars tail = chars.subchars(p + (isMatched ? ds : 0));
					return Pair.of(head, tail);
				} else
					return Pair.of(chars, Chars.emptyChars);
			}
		};
	}

}
