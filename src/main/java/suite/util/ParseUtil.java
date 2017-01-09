package suite.util;

import java.util.ArrayList;
import java.util.List;

import suite.adt.Pair;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.text.Segment;

public class ParseUtil {

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

	public static boolean isInteger(String s) {
		boolean result;

		if (!s.isEmpty()) {
			if (s.charAt(0) == '-')
				s = s.substring(1);

			result = !s.isEmpty();
			for (char c : Util.chars(s))
				result &= Character.isDigit(c);
		} else
			result = false;

		return result;
	}

	public static boolean isParseable(String s) {
		return isParseable(s, false);
	}

	/**
	 * Judges if the input string has balanced quote characters and bracket
	 * characters.
	 *
	 * @param isThrow
	 *            if this is set to true, and the string is deemed unparseable
	 *            even if more characters are added, throw exception.
	 */
	public static boolean isParseable(String s, boolean isThrow) {
		int quote = 0, depth = 0;

		// shows warning if the atom has mismatched quotes or brackets
		for (char c : Util.chars(s)) {
			quote = getQuoteChange(quote, c);
			if (quote == 0)
				depth = checkDepth(depth, c);
		}

		if (!isThrow || 0 <= depth)
			return quote == 0 && depth == 0;
		else
			throw new RuntimeException("Parse error");
	}

	public static int search(String s, int start, String toMatch) {
		int nameLength = toMatch.length();
		int end = s.length() - nameLength;
		int quote = 0;

		for (int pos = start; pos <= end; pos++) {
			char c = s.charAt(pos);
			quote = getQuoteChange(quote, c);

			if (quote == 0 && s.startsWith(toMatch, pos))
				return pos;
		}

		return -1;
	}

	public static List<String> searchn(String s, String name, Assoc assoc) {
		List<String> list = new ArrayList<>();
		Pair<String, String> pair;

		while ((pair = search(s, name, assoc)) != null) {
			list.add(pair.t0);
			s = pair.t1;
		}

		list.add(s);
		return list;
	}

	public static Pair<String, String> search(String s, String name) {
		Pair<String, String> pair = search(s, name, Assoc.RIGHT);
		return pair != null ? pair : Pair.of(s, "");
	}

	public static Pair<String, String> search(String s, String name, Assoc assoc) {
		return search(s, Segment.of(0, s.length()), name, assoc, true);
	}

	private static Pair<String, String> search(String s, Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		Segment ops = searchPosition(To.charArray(s), segment, name, assoc, isCheckDepth);

		if (ops != null) {
			String left = s.substring(segment.start, ops.start);
			String right = s.substring(ops.end, segment.end);
			return Pair.of(left, right);
		} else
			return null;
	}

	public static Segment searchPosition(char cs[], Segment segment, Operator operator) {
		return searchPosition(cs, segment, operator.getName(), operator.getAssoc(), true);
	}

	public static Segment searchPosition(char cs[], Segment segment, String name, Assoc assoc, boolean isCheckDepth) {
		int nameLength = name.length();
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

			for (int pos = pos0; pos != posx + step; pos += step) {
				char c = cs[pos];
				quote = getQuoteChange(quote, c);

				if (quote == 0) {
					if (isCheckDepth)
						depth = checkDepth(depth, c);

					if (depth == 0 && pos + nameLength <= cs.length) {
						boolean result = true; // cs.startsWith(name, pos)
						for (int i = 0; result && i < nameLength; i++)
							result &= cs[pos + i] == name.charAt(i);
						if (result)
							return Segment.of(pos, pos + nameLength);
					}
				}
			}
		}

		return null;
	}
}
