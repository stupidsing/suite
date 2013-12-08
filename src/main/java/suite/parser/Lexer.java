package suite.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import suite.util.FunUtil.Source;

public class Lexer {

	private boolean eof;
	private char peeked;
	private Reader reader;
	private Set<String> operators;

	private static final Set<String> javaOperators = new HashSet<>(Arrays.asList( //
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
			"=", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "|=", "<<=", ">>=", ">>>="));

	public Lexer(String in) {
		this(in, javaOperators);
	}

	public Lexer(String in, Set<String> operators) {
		this(new StringReader(in), javaOperators);

	}

	public Lexer(Reader reader, Set<String> operators) {
		this.reader = reader;
		this.operators = operators;
	}

	public Source<String> tokens() {
		return new Source<String>() {
			public String source() {
				return nextToken();
			}
		};
	}

	private String nextToken() {
		if (!eof) {
			boolean isEscape = false;
			char ch = peeked;

			StringBuilder sb = new StringBuilder();
			sb.append(nextChar());

			if (ch == '\'') {
				while (!eof && (isEscape || peeked != '\'')) {
					isEscape = !isEscape && peeked == '\\';
					sb.append(nextChar());
				}
				sb.append(nextChar());
			} else if (ch == '"') {
				while (!eof && (isEscape || peeked != '"')) {
					isEscape = !isEscape && peeked == '\\';
					sb.append(nextChar());
				}
				sb.append(nextChar());
			} else if (Character.isWhitespace(ch)) {
				while (!eof && Character.isWhitespace(peeked))
					sb.append(nextChar());
				return nextToken();
			} else if (Character.isDigit(ch))
				while (!eof && Character.isDigit(peeked))
					sb.append(nextChar());
			else if (Character.isJavaIdentifierStart(ch))
				while (!eof && Character.isJavaIdentifierPart(peeked))
					sb.append(nextChar());
			else
				while (!eof && operators.contains(sb.toString() + peeked))
					sb.append(nextChar());

			return sb.toString();
		} else
			return null;
	}

	public boolean eof() {
		return eof;
	}

	private char nextChar() {
		char ch = peeked;
		try {
			int read = reader.read();
			if (read >= 0)
				peeked = (char) read;
			else
				eof = true;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return ch;
	}

}
