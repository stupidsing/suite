package suite.fp;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import primal.Verbs.ReadString;
import suite.Suite;
import suite.node.io.Formatter;

public class FunRbTreeTest {

	@Test
	public void test() {
		var s = ReadString.from("src/main/fl/RB-TREE.slf");
		var fp = s + "0 until 10 | map_{rbt-insert} | apply | {Empty}\n";
		var result = Suite.evaluateFun(fp, false);
		assertNotNull(result);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

}
