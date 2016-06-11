package suite.util;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import suite.Constants;

public class XmlUtilTest {

	@Test
	public void test() throws Exception {
		String xml0 = new String(Files.readAllBytes(Paths.get("pom.xml")), Constants.charset);
		String xml1 = new XmlUtil().format(xml0);
		System.out.println(xml1);
	}

}
