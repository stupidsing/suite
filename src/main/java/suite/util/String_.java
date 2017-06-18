package suite.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

	public static String[] split(String in, String... keys) {
		List<String> outs = new ArrayList<>();
		int p = 0;
		for (String key : keys) {
			int p1 = in.indexOf(key, p);
			if (0 <= p1) {
				outs.add(in.substring(p, p1));
				p = p1 + key.length();
			} else
				return null;
		}
		outs.add(in.substring(p));
		return outs.toArray(new String[0]);
	}

	public static Pair<String, String> split2(String s, String delimiter) {
		int pos = s.indexOf(delimiter);
		if (0 <= pos)
			return Pair.of(s.substring(0, pos).trim(), s.substring(pos + delimiter.length()).trim());
		else
			return Pair.of(s.trim(), "");
	}

}
