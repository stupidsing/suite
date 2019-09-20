package suite.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import primal.Verbs.ReadFile;
import primal.Verbs.Substring;
import primal.io.ReadStream;
import suite.inspect.Dump;
import suite.util.ScrapeHtml.HtmlNode;

public class ScrapeHtmlTest {

	private ScrapeHtml sh = new ScrapeHtml();

	@Test
	public void testCdata() {
		var h = "<![CDATA[x<y]]>";
		var hn = sh.parse(h);
		System.out.println(generate(hn));
		assertEquals(h, sh.parse(h).text());
	}

	@Test
	public void testEncode() {
		assertEquals("abc & def", sh.decode("abc&nbsp;&amp;&nbsp;def"));
		assertEquals("abc&nbsp;&amp;&nbsp;def", sh.encode("abc & def"));
	}

	@Test
	public void testHtml() {
		var h = "<meta charset='utf-8'><html><!-- comment --><head></head><body>text</body></html>";
		var hn = sh.parse(h);
		System.out.println(generate(hn));
		assertEquals(h, sh.format(hn));
	}

	@Test
	public void testScript() {
		var h = "<script>if(a<b)return a;else return b;</script>";
		var hn = sh.parse(h);
		System.out.println(generate(hn));
		assertEquals("if(a<b)return a;else return b;", sh.parse(h).text());
	}

	@Test
	public void testText() {
		var h = "abc&nbsp;def<span>ghi</span>";
		var s = "abc defghi";
		assertEquals(s, sh.parse(h).text());
	}

	@Test
	public void testVariable() {
		var h = "<meta charset='utf-8'><html>{ vm.abc.def }</html>";
		var hn = sh.parse(h);
		Dump.details(hn);
		System.out.println(generate(hn));
		assertEquals(h, sh.format(hn));
	}

	@Test
	public void testRender() {
		var h = ReadFile.from("src/main/html/render.html").doRead(ReadStream::readString);
		Dump.details(sh.parse(h));
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
							sb.append(kv.k + ": '" + kv.v + "', ");
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
				} else if (h.tag.startsWith("<!--") && h.tag.endsWith("-->")) {
					sb.append("rd.dom(vm => document.createComment(");
					s(Substring.of(h.tag, 4, -3).trim());
					sb.append("))");
				} else {
					sb.append("rd.dom(vm => document.createTextNode(");
					s(h.tag);
					sb.append("))");
				}
			}

			private void s(String s) {
				var pos0 = 0;
				int pos1, pos2;

				while (0 <= (pos1 = s.indexOf("{", pos0)) && 0 <= (pos2 = s.indexOf("}", pos1))) {
					if (c(s.substring(pos0, pos1)))
						sb.append(" + ");
					sb.append(s.substring(pos1 + 1, pos2).trim() + " + ");
					pos0 = pos2 + 1;
				}

				if (!c(s.substring(pos0))) {
					var length = sb.length();
					sb.delete(length - 3, length);
				}
			}

			private boolean c(String s) {
				var b = !s.isEmpty();
				if (b)
					sb.append("'" + s.replace("'", "\'").replace("\n", "\\n") + "'");
				return b;
			}
		}.g(h);

		return sb.toString();
	}

}
