package suite.util;

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
			for (char c : s.toCharArray())
				result &= Character.isDigit(c);
		} else
			result = false;

		return result;
	}

	public static boolean isParseable(String s) {
		int quote = 0, depth = 0;

		// Shows warning if the atom has mismatched quotes or brackets
		for (char c : s.toCharArray()) {
			quote = ParseUtil.getQuoteChange(quote, c);
			if (quote == 0)
				depth = ParseUtil.checkDepth(depth, c);
		}

		return quote == 0 && depth == 0;
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

	public static String[] search(String s, Operator operator) {
		return search(s, operator.getName(), operator.getAssoc());
	}

	public static String[] search(String s, String name, Assoc assoc) {
		return search(s, name, assoc, true);
	}

	public static String[] search(String s, String name, Assoc assoc, boolean isCheckDepth) {
		boolean isLeftAssoc = assoc == Assoc.LEFT;
		int nameLength = name.length();
		int end = s.length() - nameLength;
		int quote = 0, depth = 0;

		for (int i = 0; i <= end; i++) {
			int pos = isLeftAssoc ? end - i : i;
			char c = s.charAt(pos + (isLeftAssoc ? nameLength - 1 : 0));
			quote = getQuoteChange(quote, c);

			if (quote == 0) {
				if (isCheckDepth)
					depth = ParseUtil.checkDepth(depth, c);

				if (depth == 0 && s.startsWith(name, pos)) {
					String left = s.substring(0, pos);
					String right = s.substring(pos + nameLength);
					return new String[] { left, right };
				}
			}
		}

		return null;
	}

}
