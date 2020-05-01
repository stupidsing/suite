package suite.node.pp;

import org.junit.jupiter.api.Test;
import primal.Verbs.ReadString;
import suite.Suite;

public class PrettyPrinterTest {

	private PrettyPrinter prettyPrinter = new PrettyPrinter();

	@Test
	public void test0() {
		System.out.println(prettyPrinter.prettyPrint(Suite.parse(ReadString.from("src/main/ll/fc/fc.sl"))));
	}

	@Test
	public void test1() {
		System.out.println(prettyPrinter.prettyPrint(Suite.parse(ReadString.from("src/main/fl/STANDARD.slf"))));
	}

}
