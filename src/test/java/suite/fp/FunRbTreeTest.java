package suite.fp;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.Suite;
import suite.node.io.Formatter;
import suite.os.FileUtil;

public class FunRbTreeTest {

	@Test
	public void test() {
		var s = FileUtil.read("src/main/fl/RB-TREE.slf");
		var fp = s + "0 until 10 | map_{rbt-insert} | apply | {Empty}\n";
		var result = Suite.evaluateFun(fp, false);
		assertNotNull(result);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

}
