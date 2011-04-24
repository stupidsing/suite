package org.util;

public class ParserUtil {

	public static int getQuoteChange(int quote, char c) {
		if (c == quote)
			quote = 0;
		else if (c == '\'' || c == '"')
			quote = c;
		return quote;
	}

	public static int getDepthChange(String s) {
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

}
