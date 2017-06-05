package suite.node.pp;

import org.junit.Test;

import suite.Suite;
import suite.os.FileUtil;

public class NewPrettyPrinterTest {

	private NewPrettyPrinter newPrettyPrinter = new NewPrettyPrinter();

	@Test
	public void test() {
		System.out.println(newPrettyPrinter.prettyPrint(Suite.parse(FileUtil.read("src/main/ll/fc/fc.sl"))));
	}

}
