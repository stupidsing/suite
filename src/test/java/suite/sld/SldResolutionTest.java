package suite.sld;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class SldResolutionTest {

	@Test
	public void test() {
		Node node = Suite.parse("AND (OR (VAR A) (VAR B)) (OR (NOT (VAR A)) (VAR C))");
		assertNotNull(new SldResolution().resolve(node));
	}

}
