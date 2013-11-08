package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class UsingTest {

	@Test
	public void test() {
		assertEquals(Int.create(3), eval("using MATH >> gcd {6} {9}"));
		assertEquals(Int.create(3), eval("define gcd1 = (using MATH >> gcd) >> gcd1 {6} {9}"));
		assertEquals(Int.create(3), eval("define gcd6 = (using MATH >> gcd {6}) >> gcd6 {9}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
