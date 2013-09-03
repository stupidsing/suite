package suite.lp.doer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.util.Complexity;

public class ComplexityTest {

	@Test
	public void test() {
		Complexity complexity = new Complexity();
		assertEquals(2, complexity.complexity(Suite.parse("1 + 2 * 3")));
		assertEquals(4, complexity.complexity(Suite.parse("1 * 2 * 3 * 4 + 5")));
		assertEquals(6, complexity.complexity(Suite.parse("0, 1, 2, 3, 4, 5,")));
	}

}
