package suite.fp.eval;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.util.To;

public class FunRbTreeTest {

	// Type check take 11 seconds
	@Test
	public void test() throws IOException {
		String s = To.string(getClass().getResourceAsStream("/RB-TREE.slf"));
		String fp = s + "0 until 10 | map {rbt-insert} | apply | {Empty}\n";
		Node result = Suite.evaluateFun(fp, false);
		assertNotNull(result);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

}
