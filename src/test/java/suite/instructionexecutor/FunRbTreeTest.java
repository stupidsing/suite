package suite.instructionexecutor;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import suite.lp.Suite;
import suite.lp.doer.Formatter;
import suite.lp.node.Node;
import suite.util.IoUtil;

public class FunRbTreeTest {

	// Type check take 11 seconds
	@Test
	public void test() throws IOException {
		String s = IoUtil.readStream(getClass().getResourceAsStream("/RB-TREE.slf"));
		String fp = s + "0 until 10 | map {rbt-add} | apply | {EMPTY}\n";
		Node result = Suite.evaluateFun(fp, false);
		assertNotNull(result);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

}
