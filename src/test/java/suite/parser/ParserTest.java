package suite.parser;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.Suite;
import suite.node.Tree;

public class ParserTest {

	@Test
	public void testParse() {
		assertNotNull(Tree.decompose(Suite.parse("!, a")).getLeft());
	}

}
