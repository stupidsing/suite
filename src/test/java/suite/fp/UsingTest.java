package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class UsingTest {

	@Test
	public void test() {
		assertEquals(Int.of(3), eval("use MATH ~ gcd {6} {9}"));
		assertEquals(Int.of(3), eval("define gcd1 := (use MATH ~ gcd) ~ gcd1 {6} {9}"));
		assertEquals(Int.of(3), eval("define gcd6 := (use MATH ~ gcd {6}) ~ gcd6 {9}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
