package suite.algo;

import suite.util.To;
import suite.util.Util;

public class SoundEx {

	private static final String CHARS0 = "AEIOUYHW";
	private static final String CHARS1 = "BFPV";
	private static final String CHARS2 = "CGJKQSXZ";
	private static final String CHARS3 = "DT";
	private static final String CHARS4 = "L";
	private static final String CHARS5 = "MN";
	private static final String CHARS6 = "R";

	public String american(String word) {
		word = word.toUpperCase();

		StringBuilder sb = new StringBuilder();
		char a[] = To.charArray(word);
		int len = a.length;
		int p = 0;

		while (p < len) {
			int p1 = p;
			while (p1 < len)
				if (p1 + 1 < len //
						&& index(a[p]) == index(a[p1 + 1]))
					p1++;
				else if (p1 + 2 < len //
						&& "HW".indexOf(a[p1 + 1]) >= 0 //
						&& index(a[p]) == index(a[p1 + 2]))
					p1 += 2;
				else
					break;
			sb.append(word.charAt(p));
			p = p1 + 1;
		}

		word = sb.toString();
		char first = word.charAt(0);
		String word1 = word.substring(1);

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
		StringBuilder sb = new StringBuilder();
		for (char ch : Util.chars(word))
			sb.append(match.indexOf(ch) < 0 ? ch : replace);
		word = sb.toString();
		return word;
	}

	private int index(char ch) {
		if (CHARS0.indexOf(ch) >= 0)
			return 0;
		else if (CHARS1.indexOf(ch) >= 0)
			return 1;
		else if (CHARS2.indexOf(ch) >= 0)
			return 2;
		else if (CHARS3.indexOf(ch) >= 0)
			return 3;
		else if (CHARS4.indexOf(ch) >= 0)
			return 4;
		else if (CHARS5.indexOf(ch) >= 0)
			return 5;
		else if (CHARS6.indexOf(ch) >= 0)
			return 6;
		else
			throw new RuntimeException("Unknown soundex character " + ch);
	}

}
