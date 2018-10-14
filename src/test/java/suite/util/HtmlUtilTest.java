package suite.util;

import org.junit.Test;

import suite.inspect.Dump;
import suite.os.FileUtil;

public class HtmlUtilTest {

	private HtmlUtil html = new HtmlUtil();

	@Test
	public void test0() {
		var h = "<meta charset='utf-8'><html></html>";
		Dump.details(html.parse(h));
	}

	@Test
	public void testHtml() {
		var h = "<meta charset='utf-8'><html><!-- comment --><head/><body></body></html>";
		Dump.details(html.parse(h));
	}

	@Test
	public void testRender() {
		var h = FileUtil.in("src/main/html/render.html").doRead(ReadStream::readString);
		Dump.details(html.parse(h));
	}

}
