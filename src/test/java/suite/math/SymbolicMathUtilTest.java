package suite.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;

public class SymbolicMathUtilTest {

	@Test
	public void test() {
		assertEquals(Int.of(3), SymbolicMathUtil.simplify(Suite.parse("1 + 2")));
	}

}
