package suite.text.wildcard;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import suite.text.wildcard.WildcardUtil;

public class WildcardUtilTest {

	@Test
	public void test() {
		String match[] = WildcardUtil.match("*def*jkl*", "abcdefghijklmno");
		System.out.println(Arrays.toString(match));
		assertTrue(Arrays.equals(match, new String[] { "abc", "ghi", "mno" }));
	}

}
