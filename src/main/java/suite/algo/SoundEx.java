package suite.algo;

import static suite.util.Friends.fail;

import suite.util.String_;

public class SoundEx {

	private static String CHARS0 = "AEIOUYHW";
	private static String CHARS1 = "BFPV";
	private static String CHARS2 = "CGJKQSXZ";
	private static String CHARS3 = "DT";
	private static String CHARS4 = "L";
	private static String CHARS5 = "MN";
	private static String CHARS6 = "R";

	public String american(String word) {
		word = word.toUpperCase();

		var sb = new StringBuilder();
		var a = word.toCharArray();
		var len = a.length;
		var p = 0;

		while (p < len) {
			var p1 = p;
			while (p1 < len)
				if (p1 + 1 < len //
						&& index(a[p]) == index(a[p1 + 1]))
					p1++;
				else if (p1 + 2 < len //
						&& 0 <= "HW".indexOf(a[p1 + 1]) //
						&& index(a[p]) == index(a[p1 + 2]))
					p1 += 2;
				else
					break;
			sb.append(word.charAt(p));
			p = p1 + 1;
		}

		word = sb.toString();
		var first = word.charAt(0);
		var word1 = word.substring(1);

		word1 = replace(word1, CHARS0, "");
		word1 = replace(word1, CHARS1, "1");
		word1 = replace(word1, CHARS2, "2");
		word1 = replace(word1, CHARS3, "3");
		word1 = replace(word1, CHARS4, "4");
		word1 = replace(word1, CHARS5, "5");
		word1 = replace(word1, CHARS6, "6");

		while (word1.length() < 3)
			word1 += "0";
		if (3 < word1.length())
			word1 = word1.substring(0, 3);

		return first + word1;
	}

	private String replace(String word, String match, String replace) {
		var sb = new StringBuilder();
		for (var ch : String_.chars(word))
			sb.append(match.indexOf(ch) < 0 ? ch : replace);
		word = sb.toString();
		return word;
	}

	private int index(char ch) {
		if (0 <= CHARS0.indexOf(ch))
			return 0;
		else if (0 <= CHARS1.indexOf(ch))
			return 1;
		else if (0 <= CHARS2.indexOf(ch))
			return 2;
		else if (0 <= CHARS3.indexOf(ch))
			return 3;
		else if (0 <= CHARS4.indexOf(ch))
			return 4;
		else if (0 <= CHARS5.indexOf(ch))
			return 5;
		else if (0 <= CHARS6.indexOf(ch))
			return 6;
		else
			return fail("unknown soundex character " + ch);
	}

}
