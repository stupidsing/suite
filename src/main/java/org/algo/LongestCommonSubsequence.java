package org.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.util.Util;

public class LongestCommonSubsequence {

	public static <T> List<T> lcs(List<T> l1, List<T> l2) {
		List<T> empty = Collections.emptyList();
		int size1 = l1.size(), size2 = l2.size();

		@SuppressWarnings("unchecked")
		List<T> dp[][] = (List<T>[][]) new List<?>[size1][size2];

		for (int i1 = 0; i1 < size1; i1++)
			for (int i2 = 0; i2 < size2; i2++) {
				List<T> u = i1 > 0 ? dp[i1 - 1][i2] : empty;
				List<T> l = i2 > 0 ? dp[i1][i2 - 1] : empty;
				List<T> lu = i1 > 0 && i2 > 0 ? dp[i1 - 1][i2 - 1] : empty;

				if (Util.equals(l1.get(i1), l2.get(i2)))
					(lu = new ArrayList<>(lu)).add(l1.get(i1));

				List<T> longest = u;
				if (l.size() > longest.size())
					longest = l;
				if (lu.size() > longest.size())
					longest = lu;

				dp[i1][i2] = longest;
			}

		return dp[size1 - 1][size2 - 1];
	}

}
