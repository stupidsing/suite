package suite.fp;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.os.FileUtil;

public class FunRbTreeTest {

	@Test
	public void test() throws IOException {
		String s = FileUtil.read("src/main/fl/RB-TREE.slf");
		String fp = s + "0 until 10 | map {rbt-insert} | apply | {Empty}\n";
		Node result = Suite.evaluateFun(fp, false);
		assertNotNull(result);
		System.out.println("OUT:\n" + Formatter.dump(result));
	}

}
