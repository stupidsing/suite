package org.util;

public class WildcardUtil {

	public static boolean match(String pattern, String s) {
		if (!pattern.isEmpty()) {
			char ph = pattern.charAt(0);
			String pt = pattern.substring(1);

			if (ph != '*')
				return !s.isEmpty() //
						&& s.charAt(0) == ph //
						&& match(pt, s.substring(1));
			else
				return match(pt, s) //
						|| !s.isEmpty() && match(pt, s.substring(1));
		} else
			return s.isEmpty();
	}

	public static boolean match2(String p0, String p1) {
		if (!p0.isEmpty() && !p1.isEmpty()) {
			char h0 = p0.charAt(0), h1 = p1.charAt(0);
			String t0 = p0.substring(1), t1 = p1.substring(1);

			return h0 == '*' && (match2(t0, p1) || match2(p0, t1)) //
					|| h1 == '*' && (match2(p0, t1) || match2(t0, p1)) //
					|| h0 == h1 && match2(t0, t1);
		} else {
			boolean isWildcardPatterns = true;
			for (char c0 : p0.toCharArray())
				isWildcardPatterns &= c0 == '*';
			for (char c1 : p1.toCharArray())
				isWildcardPatterns &= c1 == '*';
			return isWildcardPatterns;
		}
	}

}
