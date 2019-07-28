package suite.algo;

import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

// Find the longest palindromic substring.
// https://www.akalin.com/longest-palindrome-linear-time
public class ManacherTest {

	@Test
	public void test() {
		var mpl = manacher("abababa");
		System.out.println(mpl);
		assertEquals(7, mpl.get(7).intValue());
	}

	public List<Integer> manacher(String seq) {
		var mpls = new ArrayList<Integer>(); // maximum palindrome lengths
		var length = seq.length();
		var i = 0;
		var palindromeLength = 0;

		while (i < length)
			if (palindromeLength < i && seq.charAt(i - palindromeLength - 1) == seq.charAt(i)) {
				palindromeLength += 2;
				i++;
			} else {
				mpls.add(palindromeLength);

				var s = mpls.size() - 2;
				var e = s - palindromeLength;
				var j = s;

				while (true) {
					if (e < j) {
						var mpl = mpls.get(j);
						var d = j - e - 1;
						if (mpl != d) {
							mpls.add(min(mpl, d));
							j--;
							continue;
						} else
							palindromeLength = d;
					} else {
						palindromeLength = 1;
						i++;
					}
					break;
				}
			}

		mpls.add(palindromeLength);

		var nMpls = mpls.size();
		var s = nMpls - 2;
		var e = s - (2 * length + 1 - nMpls);

		for (var j = s; e < j; j--) {
			var mpl = mpls.get(j);
			var d = j - e - 1;
			mpls.add(min(d, mpl));
		}

		return mpls;
	}

}
