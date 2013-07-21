package suite.lp.doer;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.lp.Suite;
import suite.lp.node.Tree;

public class ParserTest {

	@Test
	public void testParse() {
		assertNotNull(Tree.decompose(Suite.parse("!, a")).getLeft());
	}

}
