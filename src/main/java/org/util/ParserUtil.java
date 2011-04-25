package org.util;

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

}
