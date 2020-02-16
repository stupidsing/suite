package suite.node.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;

public class ComplexityTest {

	@Test
	public void test() {
		var complexity = new Complexity();
		assertEquals(2, complexity.complexity(Suite.parse("1 + 2 * 3")));
		assertEquals(4, complexity.complexity(Suite.parse("1 * 2 * 3 * 4 + 5")));
		assertEquals(6, complexity.complexity(Suite.parse("0, 1, 2, 3, 4, 5,")));
	}

}
