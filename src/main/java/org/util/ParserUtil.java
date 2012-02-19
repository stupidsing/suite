package org.util;

import org.parser.Operator;
import org.parser.Operator.Assoc;

public class ParserUtil {

	public static int getDepthDelta(String s) {
		int depth = 0;
		for (char c : s.toCharArray())
			depth = checkDepth(depth, c);
		return depth;
	}

	public static boolean isPositiveDepth(String s) {
		int depth = 0;
		for (char c : s.toCharArray()) {
			depth = checkDepth(depth, c);
			if (depth < 0)
				return false;
		}
		return true;
	}

	public static int checkDepth(int depth, char c) {
		if (c == '(' || c == '[' || c == '{')
			depth++;
		if (c == ')' || c == ']' || c == '}')
			depth--;
		return depth;
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

	public static int search(String s, Operator operator) {
		return search(s, operator.getName(), operator.getAssoc());
	}

	private static int search(String s, String name, Assoc assoc) {
		return search(s, name, assoc, true);
	}

	public static int search(String s, String name, Assoc assoc,
			boolean isCheckDepth) {
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
					depth = ParserUtil.checkDepth(depth, c);

				if (depth == 0 && s.startsWith(name, pos))
					return pos;
			}
		}

		return -1;
	}

	public static int getQuoteChange(int quote, char c) {
		if (c == quote)
			quote = 0;
		else if ((c == '\'' || c == '"') && quote == 0)
			quote = c;
		return quote;
	}

}
