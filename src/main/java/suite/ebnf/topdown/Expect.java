package suite.ebnf.topdown;

import static java.lang.Math.min;

import primal.Verbs.Equals;
import primal.Verbs.Range;
import suite.os.UnicodeData;

public class Expect {

	private UnicodeData unicodeData = new UnicodeData();

	public interface ExpectFun {
		public int expect(String in, int length, int start);
	}

	public ExpectFun char_ = (in, length, start) -> min(start + 1, length);

	public ExpectFun charLiteral = (in, length, start) -> {
		int pos = start, end;
		if (pos < length && in.charAt(pos) == '\'') {
			pos++;
			if (pos < length && in.charAt(pos) == '\\')
				pos++;
			if (pos < length)
				pos++;
			if (pos < length && in.charAt(pos) == '\'') {
				pos++;
				end = pos;
			} else
				end = start;
		} else
			end = start;
		return end;
	};

	public ExpectFun fail = (in, length, start) -> start;

	public ExpectFun identifier = (in, length, start) -> {
		var pos = start;
		if (pos < length && Character.isJavaIdentifierStart(in.charAt(pos))) {
			pos++;
			while (pos < length && Character.isJavaIdentifierPart(in.charAt(pos)))
				pos++;
		}
		return pos;
	};

	public ExpectFun integerLiteral = (in, length, start) -> {
		var pos = start;
		while (pos < length && Character.isDigit(in.charAt(pos)))
			pos++;
		if (pos < length && in.charAt(pos) == 'l')
			pos++;
		return pos;
	};

	public ExpectFun realLiteral = (in, length, start) -> {
		var pos = start;
		pos = integerLiteral.expect(in, length, pos);
		if (pos < length && in.charAt(pos) == '.') {
			pos++;
			pos = integerLiteral.expect(in, length, pos);
		}
		if (pos < length && 0 <= "fd".indexOf(in.charAt(pos)))
			pos++;
		return pos;
	};

	public ExpectFun stringLiteral = (in, length, start) -> {
		int pos = start, end;
		if (pos < length && in.charAt(pos) == '"') {
			pos++;
			char c;

			while (pos < length && (c = in.charAt(pos)) != '"') {
				pos++;
				if (pos < length && c == '\\')
					pos++;
			}

			if (pos < length && in.charAt(pos) == '"') {
				pos++;
				end = pos;
			} else
				end = start;
		} else
			end = start;
		return end;
	};

	public ExpectFun charRange(char s, char e) {
		return (in, length, start) -> {
			int end;
			if (start < length) {
				var ch = in.charAt(start);
				end = start + (s <= ch && ch <= e ? 1 : 0);
			} else
				end = start;
			return end;
		};
	}

	public int comment(String in, int length, int start, String sm, String em) {
		int sl = sm.length(), el = em.length();
		int pos = start, end;
		if (pos < length && Equals.string(Range.of(in, pos, pos + sl), sm)) {
			pos += 2;
			while (pos < length && !Equals.string(Range.of(in, pos, pos + el), em))
				pos++;
			if (pos < length && Equals.string(Range.of(in, pos, pos + el), em)) {
				pos += 2;
				end = pos;
			} else
				end = start;
		} else
			end = start;
		return end;
	}

	public ExpectFun string(String s) {
		return (in, length, start) -> start + (in.startsWith(s, start) ? s.length() : 0);
	};

	public ExpectFun unicodeClass(String uc) {
		var chars = unicodeData.getCharsOfClass(uc);
		return (in, length, start) -> {
			int end;
			if (start < length) {
				var ch = in.charAt(start);
				end = start + (chars.contains(ch) ? 1 : 0);
			} else
				end = start;
			return end;
		};

	}

	public int whitespaces(String in, int length, int start) {
		int pos = start, pos1;
		while (pos < (pos1 = whitespace(in, length, pos)))
			pos = pos1;
		return pos;
	};

	public int whitespace(String in, int length, int start) {
		var pos = start;
		while (pos < length && Character.isWhitespace(in.charAt(pos)))
			pos++;
		pos = comment(in, length, pos, "/*", "*/");
		pos = comment(in, length, pos, "//", "\n");
		return pos;
	}

}
