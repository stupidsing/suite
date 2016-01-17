package suite.node.pp;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.os.FileUtil;

public class PrettyPrinterTest {

	private PrettyPrinter prettyPrinter = new PrettyPrinter();

	@Test
	public void test0() throws IOException {
		System.out.println(prettyPrinter.prettyPrint(Suite.parse(FileUtil.read("src/main/ll/fc/fc.sl"))));
	}

	@Test
	public void test1() throws IOException {
		System.out.println(prettyPrinter.prettyPrint(Suite.parse(FileUtil.read("src/main/fl/STANDARD.slf"))));
	}

}
