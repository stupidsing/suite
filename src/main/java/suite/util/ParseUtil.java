package suite.util;

import static suite.util.Friends.fail;

import java.util.ArrayList;

import suite.adt.pair.FixieArray;
import suite.adt.pair.Pair;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.text.Segment;

public class ParseUtil {

	public static FixieArray<String> fit(String in, String... parts) {
		return fit(in, s -> s, parts);
	}

	public static FixieArray<String> fitCaseInsensitive(String in, String... parts) {
		return fit(in, String::toLowerCase, parts);
	}

	public static FixieArray<String> fit(String in, Iterate<String> lower, String... parts) {
		var outs = new ArrayList<String>();
		var inl = lower.apply(in);
		var p = 0;
		for (var part : parts) {
			var p1 = inl.indexOf(lower.apply(part), p);
			if (0 <= p1) {
				outs.add(in.substring(p, p1));
				p = p1 + part.length();
			} else
				return null;
		}
		outs.add(in.substring(p));
		return FixieArray.of(outs);
	}

	public static Streamlet<String> searchn(String s, String name, Assoc assoc) {
		var pair = iter(s, name, assoc);
		return pair.t0.snoc(pair.t1);
	}

	public static Streamlet<String> splitn(String s, String name, Assoc assoc) {
		return iter(s, name, assoc).t0;
	}

	private static Pair<Streamlet<String>, String> iter(String s, String name, Assoc assoc) {
		var list = new ArrayList<String>();
		Pair<String, String> pair;

		while ((pair = search(s, name, assoc)) != null) {
			list.add(pair.t0);
			s = pair.t1;
		}

		return Pair.of(Read.from(list), s);
	}

	public static int search(String s, int start, String toMatch) {
		var nameLength = toMatch.length();
		var end = s.length() - nameLength;
		var quote = 0;

		for (var pos = start; pos <= end; pos++) {
			var c = s.charAt(pos);
			quote = getQuoteChange(quote, c);

			if (quote == 0 && s.startsWith(toMatch, pos))
				return pos;
		}

		return -1;
	}

	public static Pair<String, String> search(String s, String name, Assoc assoc) {
		return search(s, Segment.of(0, s.length()), name, assoc, true);
	}

	private static Pair<String, String> search(String s, Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		var ops = searchPosition(s.toCharArray(), segment, name, assoc, isCheckDepth);

		if (ops != null) {
			var left = s.substring(segment.start, ops.start);
			var right = s.substring(ops.end, segment.end);
			return Pair.of(left, right);
		} else
			return null;
	}

	public static Segment searchPosition(char[] cs, Segment segment, Operator operator) {
		return searchPosition(cs, segment, operator.name_(), operator.assoc(), true);
	}

	public static Segment searchPosition(char[] cs, Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		var nameLength = name.length();
		int start1 = segment.start, end1 = segment.end - 1;
		int quote = 0, depth = 0;
		int pos0, posx, step;

		if (start1 <= end1) {
			if (assoc == Assoc.RIGHT) {
				pos0 = start1;
				posx = end1;
				step = 1;
			} else {
				pos0 = end1;
				posx = start1;
				step = -1;
			}

			for (var pos = pos0; pos != posx + step; pos += step) {
				var c = cs[pos];
				quote = getQuoteChange(quote, c);

				if (quote == 0) {
					if (isCheckDepth)
						depth = checkDepth(depth, c);

					if (depth == 0 && pos + nameLength <= cs.length) {
						var b = true; // cs.startsWith(name, pos)
						for (var i = 0; b && i < nameLength; i++)
							b &= cs[pos + i] == name.charAt(i);
						if (b)
							return Segment.of(pos, pos + nameLength);
					}
				}
			}
		}

		return null;
	}

	public static boolean isParseable(String s) {
		return isParseable(s, false);
	}

	/**
	 * Judges if the input string has balanced quote characters and bracket
	 * characters.
	 *
	 * @param isThrow
	 *                    if this is set to true, and the string is deemed
	 *                    unparseable even if more characters are added, throw
	 *                    exception.
	 */
	public static boolean isParseable(String s, boolean isThrow) {
		int quote = 0, depth = 0;

		// shows warning if the atom has mismatched quotes or brackets
		for (var c : String_.chars(s)) {
			quote = getQuoteChange(quote, c);
			if (quote == 0)
				depth = checkDepth(depth, c);
		}

		return !isThrow || 0 <= depth ? quote == 0 && depth == 0 : fail("parse error");
	}

	private static int checkDepth(int depth, char c) {
		if (c == '(' || c == '[' || c == '{')
			depth++;
		if (c == ')' || c == ']' || c == '}')
			depth--;
		return depth;
	}

	public static int getQuoteChange(int quote, char c) {
		if (c == quote)
			quote = 0;
		else if (quote == 0 && (c == '\'' || c == '"' || c == '`'))
			quote = c;
		return quote;
	}

	public static boolean isWhitespace(byte b) {
		return b == 0;
	}

	public static boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}

	public static boolean isWhitespace(double d) {
		return d == 0d;
	}

	public static boolean isWhitespace(float f) {
		return f == 0f;
	}

	public static boolean isWhitespace(int i) {
		return i == 0;
	}

}
