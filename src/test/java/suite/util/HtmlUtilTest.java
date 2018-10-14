package suite.util;

import org.junit.Test;

import suite.inspect.Dump;

public class HtmlUtilTest {

	private HtmlUtil h = new HtmlUtil();

	@Test
	public void test0() {
		var html = h.parse("<meta charset='utf-8'><html></html>");
		Dump.details(html);
	}

	@Test
	public void testHtml() {
		var html = h.parse("<meta charset='utf-8'><html><!-- comment --><head/><body></body></html>");
		Dump.details(html);
	}

}
