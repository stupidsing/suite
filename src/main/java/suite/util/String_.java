package suite.util;

import static java.lang.Math.min;

import primal.Ob;
import suite.adt.pair.Pair;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Puller;
import suite.streamlet.Streamlet;

public class String_ {

	public static String build(Sink<StringBuilder> sink) {
		var sb = new StringBuilder();
		sink.f(sb);
		return sb.toString();
	}

	public static char charAt(String s, int pos) {
		if (pos < 0)
			pos += s.length();
		return s.charAt(pos);
	}

	public static Streamlet<Character> chars(CharSequence s) {
		return new Streamlet<>(() -> Puller.of(new Source<>() {
			private int index = 0;

			public Character g() {
				return index < s.length() ? s.charAt(index++) : null;
			}
		}));
	}

	public static int compare(String s0, String s1) {
		return Ob.compare(s0, s1);
	}

	public static boolean equals(String s0, String s1) {
		return Ob.equals(s0, s1);
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

	public static String left(String s, int pos) {
		var size = s.length();
		if (pos < 0)
			pos += size;
		return s.substring(0, pos);
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
