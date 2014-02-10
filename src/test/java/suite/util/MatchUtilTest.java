package suite.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class MatchUtilTest {

	@Test
	public void test() {
		String match[] = new MatchUtil().match("*def*jkl*", "abcdefghijklmno");
		System.out.println(Arrays.toString(match));
		assertTrue(Arrays.equals(match, new String[] { "abc", "ghi", "mno" }));
	}

}
