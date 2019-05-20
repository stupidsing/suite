package suite.algo;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

// https://stackoverflow.com/questions/9452701/ukkonens-suffix-tree-algorithm-in-plain-english/9513423#9513423
public class SuffixTreeTest {

	@Test
	public void test() {
		var words = new String[] { //
				"abcabcabc$", //
				"abc$", //
				"abcabxabcd$", //
				"abcabxabda$", //
				"abcabxad$", //
				"aabaaabb$", //
				"aababcabcd$", //
				"ababcabcd$", //
				"abccba$", //
				"mississipi$", //
				"abacabadabacabae$", //
				"abcabcd$", //
				"00132220$" };

		Arrays.stream(words).forEach(word -> {
			var suffixTree = new SuffixTree(word);
			System.out.println("Building suffix tree for word: " + word);
			System.out.println("Suffix tree: " + suffixTree.root);
			for (var i = 0; i < word.length(); i++)
				assertTrue(suffixTree.root.contains(word.substring(i)));
		});
	}

}
