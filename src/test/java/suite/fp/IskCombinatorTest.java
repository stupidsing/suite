package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Node;

public class IskCombinatorTest {

	private String isk = """
			define i := x => x ~
			define k := x => y => x ~
			define s := x => y => z => x_{z}_{y_{z}} ~
			""";

	@Test
	public void testSksk() {
		var sksk = "s_{k}_{s}_{k}";
		assertEquals(Suite.parse("1"), eval(isk + "(" + sksk + ")_{1}_{2}"));
	}

	@Test
	public void testTf() {
		var tf = """
				define t := k ~
				define f := k_{i} ~
				define not_ := f_{t} ~
				define or_ := k ~
				define and_ := f ~
				""";

		assertEquals(Suite.parse("1"), eval(isk + tf + "t_{1}_{2}"));
		assertEquals(Suite.parse("2"), eval(isk + tf + "f_{1}_{2}"));

		// eval(isk + tf + "t_{or_}_{f}") becomes t
		// eval(isk + tf + "t_{or_}_{f}") becomes f
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, false);
	}

}
