package suite.util;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.xml.sax.SAXException;

import suite.os.FileUtil;
import suite.util.XmlUtil.XmlNode;

public class XmlUtilTest {

	private XmlUtil xmlUtil = new XmlUtil();

	@Test
	public void test() throws Exception {
		var xml0 = To.string(Files.readAllBytes(Paths.get("pom.xml")));
		var xml1 = xmlUtil.format(xml0);
		System.out.println(xml1);
	}

	@Test
	public void test1() throws SAXException {
		XmlNode xml = xmlUtil.read(FileUtil.in("pom.xml"));

		System.out.println(xml);
		System.out.println(xml.children("project").uniqueResult().toString());

		xml //
				.children("project") //
				.uniqueResult() //
				.children("dependencies") //
				.uniqueResult() //
				.children("dependency") //
				.forEach(d -> System.out.println(d.nodeName()));
	}

}
