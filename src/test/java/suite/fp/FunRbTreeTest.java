package suite.fp;

import org.junit.jupiter.api.Test;
import primal.Verbs.ReadString;
import suite.Suite;
import suite.node.io.Formatter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
