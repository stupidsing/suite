package org.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.suite.Suite;
import org.suite.node.Node;

public class MathUtilTest {

	@Test
	public void test() {
		assertEquals(Node.num(3), MathUtil.simplify(Suite.parse("1 + 2")));
	}

}
