package suite.util;

import static suite.util.Friends.min;

import java.util.Iterator;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.object.Object_;

public class String_ {

	public static char charAt(String s, int pos) {
		if (pos < 0)
			pos += s.length();
		return s.charAt(pos);
	}

	public static Iterable<Character> chars(CharSequence s) {
		return () -> new Iterator<>() {
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
		var isBlank = true;
		if (s != null)
			for (var c : String_.chars(s))
				isBlank &= Character.isWhitespace(c);
		return isBlank;
	}

	public static boolean isInteger(String s) {
		if (!s.isEmpty()) {
			if (s.charAt(0) == '-')
				s = s.substring(1);

			var b = !s.isEmpty();
			for (var c : String_.chars(s))
				b &= Character.isDigit(c);
			return b;
		} else
			return false;
	}

	public static boolean isNotBlank(String s) {
		return !isBlank(s);
	}

	public static String range(String s, int start, int end) {
		var length = s.length();
		if (start < 0)
			start += length;
		if (end < 0)
			end += length;
		end = min(length, end);
		return s.substring(start, end);
	}

	public static String right(String s, int pos) {
		var size = s.length();
		if (pos < 0)
			pos += size;
		return s.substring(pos);
	}

	public static Pair<String, String> split2l(String s, String delimiter) {
		var pair = split2(s, delimiter);
		return pair != null ? pair : Pair.of(s.trim(), "");
	}

	public static Pair<String, String> split2r(String s, String delimiter) {
		var pair = split2(s, delimiter);
		return pair != null ? pair : Pair.of("", s.trim());
	}

	public static Pair<String, String> split2(String s, String delimiter) {
		var pos = s.indexOf(delimiter);
		return 0 <= pos ? Pair.of(s.substring(0, pos).trim(), s.substring(pos + delimiter.length()).trim()) : null;
	}

}
