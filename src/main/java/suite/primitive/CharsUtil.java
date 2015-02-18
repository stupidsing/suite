package suite.primitive;

import java.io.IOException;
import java.io.Writer;

import suite.primitive.Chars.CharsBuilder;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class CharsUtil {

	private static final int bufferSize = 65536;

	public static Streamlet<Chars> buffer(Streamlet<Chars> st) {
		return Read.from(new Source<Chars>() {
			private Streamlet<Chars> st_ = st;
			protected Chars buffer = Chars.emptyChars;
			protected boolean isEof = false;

			public Chars source() {
				fill();
				int n = Math.min(buffer.size(), bufferSize);
				Chars head = buffer.subchars(0, n);
				buffer = buffer.subchars(n);
				return head;
			}

			private void fill() {
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				Chars chars;
				while (!isEof && cb.size() < bufferSize)
					if ((chars = st_.next()) != null)
						cb.append(chars);
					else
						isEof = true;
				buffer = cb.toChars();
			}
		});
	}

	public static Streamlet<Chars> concatSplit(Streamlet<Chars> st, Chars delim) {
		int ds = delim.size();

		return Read.from(new Source<Chars>() {
			private Chars buffer = Chars.emptyChars;
			private boolean isArriving;
			private int p;

			public Chars source() {
				Chars chars;
				CharsBuilder cb = new CharsBuilder();
				cb.append(buffer);

				p = 0;

				while (isArriving && !search(delim) && (isArriving = (chars = st.next()) != null)) {
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
		});
	}

	public static void copy(Streamlet<Chars> st, Writer writer) throws IOException {
		Chars chars;
		while ((chars = st.next()) != null)
			chars.write(writer);
	}

	public static boolean isWhiespaces(Chars chars) {
		boolean result = true;
		for (int i = chars.start; result && i < chars.end; i++)
			result &= Character.isWhitespace(chars.cs[i]);
		return result;
	}

	public static Chars trim(Chars chars) {
		char cs[] = chars.cs;
		int start = chars.start;
		int end = chars.end;
		while (start < end && Character.isWhitespace(cs[start]))
			start++;
		while (start < end && Character.isWhitespace(cs[end - 1]))
			end--;
		return Chars.of(cs, start, end);
	}

}
