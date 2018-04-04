package suite.sample;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;

public class SldResolutionTest {

	@Test
	public void test() {
		var node = Suite.parse("AND (OR (VAR A) (VAR B)) (OR (NOT (VAR A)) (VAR C))");
		List<Node> results = new SldResolution().resolve(node);
		System.out.println(results);
		assertTrue(results != Atom.NIL);
	}

}
