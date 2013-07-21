package suite.lp.doer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.lp.Suite;
import suite.lp.node.Int;
import suite.lp.node.Node;
import suite.lp.node.Reference;
import suite.util.Util;

public class FormatterTest {

	@Test
	public void testFormatter() {
		assertEquals("123", Formatter.display(Int.create(123)));
		testFormat("1 + 2 * 3");
		testFormat("1 * 2 - 3");
		testFormat("1 - 2 - 3");
		testFormat("1 + 2 + 3");
		testFormat("(1 + 2) * 3");
		testFormat("a - b - c");
		testFormat("a - (b - c)");
		testFormat("(a, b) = (c, d)");
		Util.dump(Util.currentMethod(), Formatter.display(new Reference()));
	}

	private void testFormat(String s) {
		Node node = Suite.parse(s);
		assertEquals(s, Formatter.display(node));
	}

}
