package suite.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HtmlUtilTest {

	private HtmlUtil htmlUtil = new HtmlUtil();

	@Test
	public void testEncode() {
		assertEquals("abc & def", htmlUtil.decode("abc&nbsp;&amp;&nbsp;def"));
		assertEquals("abc&nbsp;&amp;&nbsp;def", htmlUtil.encode("abc & def"));
	}

}
