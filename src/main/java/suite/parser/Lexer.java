package suite.parser;

import static suite.util.Fail.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.streamlet.FunUtil.Source;

/**
 * Lexical analyzer.
 *
 * @author ywsing
 */
public class Lexer {

	private boolean eof;
	private char peeked;
	private Reader reader;
	private Set<String> operators;

	private static Set<String> javaOperators = new HashSet<>(List.of( //
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
		this(new StringReader(in), operators);

	}

	public Lexer(Reader reader, Set<String> operators) {
		this.reader = reader;
		this.operators = operators;
		nextChar();

	}

	public Source<String> tokens() {
		return () -> !eof ? nextToken() : null;
	}

	private String nextToken() {
		if (!eof) {
			var isEscape = false;
			var ch = peeked;

			var sb = new StringBuilder();
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
			} else if (ch == '/' && peeked == '/') { // single-line comment
				while (!eof && nextChar() != '\n')
					;
				return nextToken();
			} else if (ch == '/' && peeked == '*') { // block comment
				while (!eof && nextChar() != '*' || peeked != '/')
					;
				if (!eof)
					nextChar();
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
		var ch = peeked;
		try {
			var read = reader.read();
			if (0 <= read)
				peeked = (char) read;
			else
				eof = true;
		} catch (IOException ex) {
			fail(ex);
		}
		return ch;
	}

}
