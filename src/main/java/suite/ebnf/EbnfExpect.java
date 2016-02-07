package suite.ebnf;

import java.util.Set;

import suite.os.UnicodeData;
import suite.util.Util;

public class EbnfExpect {

	private UnicodeData unicodeData = new UnicodeData();

	public interface Expect {
		public int expect(String in, int length, int start);
	}

	public Expect expectChar = (in, length, start) -> Math.min(start + 1, length);

	public Expect expectCharLiteral = (in, length, start) -> {
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

	public Expect expectFail = (in, length, start) -> start;

	public Expect expectIdentifier = (in, length, start) -> {
		int pos = start;
		if (pos < length && Character.isJavaIdentifierStart(in.charAt(pos))) {
			pos++;
			while (pos < length && Character.isJavaIdentifierPart(in.charAt(pos)))
				pos++;
		}
		return pos;
	};

	public Expect expectIntegerLiteral = (in, length, start) -> {
		int pos = start;
		while (pos < length && Character.isDigit(in.charAt(pos)))
			pos++;
		if (pos < length && in.charAt(pos) == 'l')
			pos++;
		return pos;
	};

	public Expect expectRealLiteral = (in, length, start) -> {
		int pos = start;
		pos = expectIntegerLiteral.expect(in, length, pos);
		if (pos < length && in.charAt(pos) == '.') {
			pos++;
			pos = expectIntegerLiteral.expect(in, length, pos);
		}
		if (pos < length && "fd".indexOf(in.charAt(pos)) >= 0)
			pos++;
		return pos;
	};

	public Expect expectStringLiteral = (in, length, start) -> {
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

	public Expect expectCharRange(char s, char e) {
		return (in, length, start) -> {
			int end;
			if (start < length) {
				char ch = in.charAt(start);
				end = start + (s <= ch && ch <= e ? 1 : 0);
			} else
				end = start;
			return end;
		};
	}

	public Expect expectString(String s) {
		return (in, length, start) -> start + (in.startsWith(s, start) ? s.length() : 0);
	};

	public Expect expectUnicodeClass(String uc) {
		Set<Character> chars = unicodeData.getCharsOfClass(uc);
		return (in, length, start) -> {
			int end;
			if (start < length) {
				char ch = in.charAt(start);
				end = start + (chars.contains(ch) ? 1 : 0);
			} else
				end = start;
			return end;
		};

	}

	public int expectWhitespaces(String in, int length, int start) {
		int pos = start, pos1;
		while (pos < (pos1 = expectWhitespace(in, length, pos)))
			pos = pos1;
		return pos;
	};

	public int expectWhitespace(String in, int length, int start) {
		int pos = start;
		while (pos < length && Character.isWhitespace(in.charAt(pos)))
			pos++;
		pos = expectComment(in, length, pos, "/*", "*/");
		pos = expectComment(in, length, pos, "//", "\n");
		return pos;
	}

	public int expectComment(String in, int length, int start, String sm, String em) {
		int sl = sm.length(), el = em.length();
		int pos = start, end;
		if (pos < length && Util.stringEquals(Util.substr(in, pos, pos + sl), sm)) {
			pos += 2;
			while (pos < length && !Util.stringEquals(Util.substr(in, pos, pos + el), em))
				pos++;
			if (pos < length && Util.stringEquals(Util.substr(in, pos, pos + el), em)) {
				pos += 2;
				end = pos;
			} else
				end = start;
		} else
			end = start;
		return end;
	}

}
