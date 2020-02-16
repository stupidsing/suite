package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Int;
import suite.node.Node;

public class UsingTest {

	@Test
	public void test() {
		assertEquals(Int.of(3), eval("use MATH ~ gcd_{6}_{9}"));
		assertEquals(Int.of(3), eval("define gcd1 := (use MATH ~ gcd) ~ gcd1_{6}_{9}"));
		assertEquals(Int.of(3), eval("define gcd6 := (use MATH ~ gcd_{6}) ~ gcd6_{9}"));
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
