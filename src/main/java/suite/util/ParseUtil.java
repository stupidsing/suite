package suite.util;

import java.util.ArrayList;
import java.util.List;

import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;

public class ParseUtil {

	public static int checkDepth(int depth, char c) {
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

		// Shows warning if the atom has mismatched quotes or brackets
		for (char c : Util.chars(s)) {
			quote = getQuoteChange(quote, c);
			if (quote == 0)
				depth = checkDepth(depth, c);
		}

		if (!isThrow || depth >= 0)
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

	public static Pair<String, String> search(String s, Operator operator) {
		return search(s, operator.getName(), operator.getAssoc());
	}

	public static Pair<String, String> search(String s, String name, Assoc assoc) {
		return search(s, name, assoc, true);
	}

	public static Pair<String, String> search(String s, String name, Assoc assoc, boolean isCheckDepth) {
		int position = searchPosition(s, 0, name, assoc, isCheckDepth);

		if (position < s.length()) {
			String left = s.substring(0, position);
			String right = s.substring(position + name.length());
			return Pair.of(left, right);
		} else
			return null;
	}

	public static int searchPosition(String s, int start, String name, Assoc assoc, boolean isCheckDepth) {
		int length = s.length(), nameLength = name.length();
		int end = length - nameLength, quote = 0, depth = 0;
		int pos0, posx, step;

		if (assoc == Assoc.RIGHT) {
			pos0 = start;
			posx = end;
			step = 1;
		} else {
			pos0 = end;
			posx = start;
			step = -1;
		}

		for (int pos = pos0; pos != posx; pos += step) {
			char c = s.charAt(pos);
			quote = getQuoteChange(quote, c);

			if (quote == 0) {
				if (isCheckDepth)
					depth = checkDepth(depth, c);

				if (depth == 0 && s.startsWith(name, pos))
					return pos;
			}
		}

		return length;
	}

}
