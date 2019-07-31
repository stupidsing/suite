package suite.algo;

import static primal.statics.Fail.fail;

import suite.util.String_;

public class SoundEx {

	private static String CHARS0 = "AEIOUYHW";
	private static String CHARS1 = "BFPV";
	private static String CHARS2 = "CGJKQSXZ";
	private static String CHARS3 = "DT";
	private static String CHARS4 = "L";
	private static String CHARS5 = "MN";
	private static String CHARS6 = "R";

	public String american(String word0) {
		var word1 = word0.toUpperCase();

		var word2 = String_.build(sb -> {
			var a = word1.toCharArray();
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
				sb.append(a[p]);
				p = p1 + 1;
			}
		});

		var first = word2.charAt(0);
		var word3 = word2.substring(1);

		word3 = replace(word3, CHARS0, "");
		word3 = replace(word3, CHARS1, "1");
		word3 = replace(word3, CHARS2, "2");
		word3 = replace(word3, CHARS3, "3");
		word3 = replace(word3, CHARS4, "4");
		word3 = replace(word3, CHARS5, "5");
		word3 = replace(word3, CHARS6, "6");

		while (word3.length() < 3)
			word3 += "0";

		if (3 < word3.length())
			word3 = word3.substring(0, 3);

		return first + word3;
	}

	private String replace(String word, String match, String replace) {
		return String_.build(sb -> {
			for (var ch : String_.chars(word))
				sb.append(match.indexOf(ch) < 0 ? ch : replace);
		});
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
