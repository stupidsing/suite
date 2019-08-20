package suite.node.pp;

import org.junit.Test;

import primal.Verbs.ReadString;
import suite.Suite;

public class NewPrettyPrinterTest {

	private NewPrettyPrinter newPrettyPrinter = new NewPrettyPrinter();

	@Test
	public void test() {
		System.out.println(newPrettyPrinter.prettyPrint(Suite.parse(ReadString.from("src/main/ll/fc/fc.sl"))));
	}

}
