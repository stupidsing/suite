package suite.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class WildcardTest {

	@Test
	public void test0() {
		var match = Wildcard.match("*def*jkl*", "abcdefghijklmno");
		System.out.println(Arrays.toString(match));
		assertTrue(Arrays.equals(match, new String[] { "abc", "ghi", "mno", }));
		assertEquals("abcpqrghixyzmno", Wildcard.apply("*pqr*xyz*", match));
	}

	@Test
	public void test1() {
		var match = Wildcard.matchStart("if * then ", "if a = b then if c = d then e = f else g = h");
		assertTrue(Arrays.equals(match.k, new String[] { "a = b", }));
		assertEquals("if c = d then e = f else g = h", match.v);
	}

}
