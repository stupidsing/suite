package suite.util;

import org.junit.Test;

import suite.inspect.Dump;
import suite.os.FileUtil;
import suite.util.HtmlUtil.HtmlNode;

public class HtmlUtilTest {

	private HtmlUtil html = new HtmlUtil();

	@Test
	public void test() {
		var h = "<meta charset='utf-8'><html></html>";
		var hn = html.parse(h);
		Dump.details(hn);
		System.out.println(generate(hn));
	}

	@Test
	public void testHtml() {
		var h = "<meta charset='utf-8'><html><!-- comment --><head/><body>text</body></html>";
		var hn = html.parse(h);
		Dump.details(hn);
		System.out.println(generate(hn));
	}

	@Test
	public void testRender() {
		var h = FileUtil.in("src/main/html/render.html").doRead(ReadStream::readString);
		Dump.details(html.parse(h));
	}

	private String generate(HtmlNode h) {
		var sb = new StringBuilder();

		new Object() {
			private void g(HtmlNode h) {
				if (h.name != null) {
					sb.append("rd.tag('" + h.name + "')");

					if (!h.attrs.isEmpty()) {
						sb.append(".attrsf(vm => { ");
						for (var kv : h.attrs)
							sb.append(kv.t0 + ": '" + kv.t1 + "', ");
						sb.append("})");
					}

					if (!h.children.isEmpty()) {
						sb.append(".children(");
						for (var child : h.children) {
							g(child);
							sb.append(", ");
						}
						sb.append(")");
					}
				} else if (h.tag.startsWith("<!--") && h.tag.endsWith("-->"))
					sb.append("rd.dom(vm => document.createComment('" + String_.range(h.tag, 4, -3).trim() + "'))");
				else
					sb.append("rd.dom(vm => document.createTextNode('" + h.tag + "'))");
			}
		}.g(h);

		return sb.toString();
	}

}
