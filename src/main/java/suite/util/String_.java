package suite.util;

import java.util.Iterator;
import java.util.Objects;

import suite.adt.pair.Pair;

public class String_ {

	public static char charAt(String s, int pos) {
		if (pos < 0)
			pos += s.length();
		return s.charAt(pos);
	}

	public static Iterable<Character> chars(CharSequence s) {
		return () -> new Iterator<Character>() {
			private int index = 0;

			public boolean hasNext() {
				return index < s.length();
			}

			public Character next() {
				return s.charAt(index++);
			}
		};
	}

	public static int compare(String s0, String s1) {
		return Object_.compare(s0, s1);
	}

	public static boolean equals(String s0, String s1) {
		return Objects.equals(s0, s1);
	}

	public static boolean isBlank(String s) {
		boolean isBlank = true;
		if (s != null)
			for (char c : String_.chars(s))
				isBlank &= Character.isWhitespace(c);
		return isBlank;
	}

	public static boolean isInteger(String s) {
		boolean result;

		if (!s.isEmpty()) {
			if (s.charAt(0) == '-')
				s = s.substring(1);

			result = !s.isEmpty();
			for (char c : String_.chars(s))
				result &= Character.isDigit(c);
		} else
			result = false;

		return result;
	}

	public static boolean isNotBlank(String s) {
		return !isBlank(s);
	}

	public static String range(String s, int start, int end) {
		int length = s.length();
		if (start < 0)
			start += length;
		if (end < 0)
			end += length;
		end = Math.min(length, end);
		return s.substring(start, end);
	}

	public static String right(String s, int pos) {
		int size = s.length();
		if (pos < 0)
			pos += size;
		return s.substring(pos);
	}

	public static Pair<String, String> split2(String s, String delimiter) {
		int pos = s.indexOf(delimiter);
		if (0 <= pos)
			return Pair.of(s.substring(0, pos).trim(), s.substring(pos + delimiter.length()).trim());
		else
			return Pair.of(s.trim(), "");
	}

}
