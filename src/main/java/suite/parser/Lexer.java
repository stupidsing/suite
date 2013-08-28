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
		if (position < chars.length) {
			int start = position;
			char ch = chars[position++];
			boolean isEscape = false;

			if (ch == '\'') {
				while (position < chars.length && (isEscape || chars[position] != '\'')) {
					isEscape = !isEscape && chars[position] == '\\';
					position++;
				}
				position++;
			} else if (ch == '"') {
				while (position < chars.length && (isEscape || chars[position] != '"')) {
					isEscape = !isEscape && chars[position] == '\\';
					position++;
				}
				position++;
			} else if (Character.isWhitespace(ch)) {
				while (position < chars.length && Character.isWhitespace(chars[position]))
					position++;
				return nextToken();
			} else if (Character.isDigit(ch))
				while (position < chars.length && Character.isDigit(chars[position]))
					position++;
			else if (Character.isJavaIdentifierStart(ch))
				while (position < chars.length && Character.isJavaIdentifierPart(chars[position]))
					position++;
			else
				while (position < chars.length && operators.contains(new String(chars, start, position + 1 - start)))
					position++;

			return new String(chars, start, position - start);
		} else
			return null;
	}
}
