package suite.node.pp;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.os.FileUtil;

public class NewPrettyPrinterTest {

	private NewPrettyPrinter newPrettyPrinter = new NewPrettyPrinter();

	@Test
	public void test() throws IOException {
		System.out.println(newPrettyPrinter.prettyPrint(Suite.parse(FileUtil.read("src/main/ll/fc/fc.sl"))));
	}

}
