package suite.fp;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;

public class Fun23TreeTest {

	@Test
	public void test() throws IOException {
		String list10 = "0 until 10 | map {insert} | apply | {Tree (9999, Empty;)}";

		Node fp0 = Suite.substitute("using 23-TREE >> " + list10);
		Node result0 = Suite.evaluateFun(Suite.fcc(fp0, false));
		assertNotNull(result0);
		System.out.println("OUT:\n" + Formatter.dump(result0));

		Node fp1 = Suite.substitute("using 23-TREE >> 0 until 5 | map {remove} | apply | {" + list10 + "}");
		Node result1 = Suite.evaluateFun(Suite.fcc(fp1, false));
		assertNotNull(result1);
		System.out.println("OUT:\n" + Formatter.dump(result1));
	}

}
