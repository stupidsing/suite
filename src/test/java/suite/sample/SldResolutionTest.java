package suite.sample;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Atom;

public class SldResolutionTest {

	@Test
	public void test() {
		var node = Suite.parse("AND (OR (VAR A) (VAR B)) (OR (NOT (VAR A)) (VAR C))");
		var results = new SldResolution().resolve(node);
		System.out.println(results);
		assertTrue(results != Atom.NIL);
	}

}
