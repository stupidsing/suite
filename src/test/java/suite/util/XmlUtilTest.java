package suite.util;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import suite.util.os.FileUtil;

public class XmlUtilTest {

	@Test
	public void test() throws Exception {
		String xml0 = new String(Files.readAllBytes(Paths.get("pom.xml")), FileUtil.charset);
		String xml1 = new XmlUtil().format(xml0);
		System.out.println(xml1);
	}

}
