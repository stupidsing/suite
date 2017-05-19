package suite.wildcard;

import java.util.List;

import suite.adt.pair.Pair;
import suite.util.String_;

public class WildcardUtil {

	public static boolean isMatch(String pattern, String s) {
		if (!pattern.isEmpty()) {
			char ph = pattern.charAt(0);
			String pt = pattern.substring(1);

			if (ph != '*')
				return !s.isEmpty() && s.charAt(0) == ph && isMatch(pt, s.substring(1));
			else
				return isMatch(pt, s) || !s.isEmpty() && isMatch(pattern, s.substring(1));
		} else
			return s.isEmpty();
	}

	public static boolean isMatch2(String p0, String p1) {
		if (!p0.isEmpty() && !p1.isEmpty()) {
			char h0 = p0.charAt(0), h1 = p1.charAt(0);
			String t0 = p0.substring(1), t1 = p1.substring(1);

			return h0 == '*' && (isMatch2(t0, p1) || isMatch2(p0, t1)) //
					|| h1 == '*' && (isMatch2(p0, t1) || isMatch2(t0, p1)) //
					|| h0 == h1 && isMatch2(t0, t1);
		} else {
			boolean isWildcardPatterns = true;
			for (char c0 : String_.chars(p0))
				isWildcardPatterns &= c0 == '*';
			for (char c1 : String_.chars(p1))
				isWildcardPatterns &= c1 == '*';
			return isWildcardPatterns;
		}
	}

	public static String[] match(String pattern, String input) {
		List<String[]> matches = matches(pattern, input);
		return matches.size() == 1 ? matches.get(0) : null;
	}

	public static List<String[]> matches(String pattern, String input) {
		return new Matcher().matches(pattern, input);
	}

	public static Pair<String[], String> matchStart(String pattern, String input) {
		return new Matcher().matchStart(pattern, input);
	}

	public static String apply(String pattern, String[] input) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (char ch : String_.chars(pattern))
			switch (ch) {
			case '*':
			case '?':
				sb.append(input[i++]);
				break;
			default:
				sb.append(ch);
			}
		return sb.toString();
	}

}
