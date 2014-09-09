package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.immutable.IMap;
import suite.node.Int;

public class LazyFunInterpreterTest {

	@Test
	public void test() {
		String expr = "sum := (n => if (n != 0) then (n + sum {n - 1}) else 0) >> sum {10}";
		assertEquals(Int.of(55), new LazyFunInterpreter().lazy(Suite.parse(expr), new IMap<>()).source());
	}

}
