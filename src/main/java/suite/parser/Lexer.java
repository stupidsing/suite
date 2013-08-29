// '\''
package suite.parser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Lexer {

	private char chars[];
	private int position = 0;

	private List<String> operators = Arrays.asList( //
			"++", "--", "+", "-", "~", "!", //
			"*", "/", "%", //
			"+", "-", //
			"<<", ">>", ">>>", //
			"<", ">", "<=", ">=", "instanceof", //
			"==", "!=", //
			"&", //
			"^", //
			"|", //
			"&&", //
			"||", //
			"?", ":", //
			"=", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "<<=", ">>=", ">>>=");

	public Lexer(String in) {
		this.chars = in.toCharArray();
	}

	public Iterable<String> tokens() {
		final Iterator<String> iterator = new Iterator<String>() {
			private String nextToken = nextToken();

			public boolean hasNext() {
				return nextToken != null;
			}

			@Override
			public String next() {
				String result = nextToken;
				nextToken = nextToken();
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};

		return new Iterable<String>() {
			public Iterator<String> iterator() {
				return iterator;
			}
		};
	}

	private String nextToken() {
		if (!eof()) {
			int start = position;
			boolean isEscape = false;
			char ch = peekChar();

			StringBuilder sb = new StringBuilder();
			sb.append(nextChar());

			if (ch == '\'') {
				while (!eof() && (isEscape || peekChar() != '\'')) {
					isEscape = !isEscape && peekChar() == '\\';
					sb.append(nextChar());
				}
				sb.append(nextChar());
			} else if (ch == '"') {
				while (!eof() && (isEscape || peekChar() != '"')) {
					isEscape = !isEscape && peekChar() == '\\';
					sb.append(nextChar());
				}
				sb.append(nextChar());
			} else if (Character.isWhitespace(ch)) {
				while (!eof() && Character.isWhitespace(peekChar()))
					sb.append(nextChar());
				return nextToken();
			} else if (Character.isDigit(ch))
				while (!eof() && Character.isDigit(peekChar()))
					sb.append(nextChar());
			else if (Character.isJavaIdentifierStart(ch))
				while (!eof() && Character.isJavaIdentifierPart(peekChar()))
					sb.append(nextChar());
			else
				while (!eof() && operators.contains(new String(chars, start, position + 1 - start)))
					sb.append(nextChar());

			return sb.toString();
		} else
			return null;
	}

	private boolean eof() {
		return position >= chars.length;
	}

	private char peekChar() {
		return chars[position];
	}

	private char nextChar() {
		return chars[position++];
	}

}
