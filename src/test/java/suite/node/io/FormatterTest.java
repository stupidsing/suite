package suite.node.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.inspect.Dump;
import suite.node.Int;
import suite.node.Reference;
import suite.util.Thread_;

public class FormatterTest {

	@Test
	public void testDisplay() {
		assertEquals("123", Formatter.display(Int.of(123)));
		testDisplay("1 + 2 * 3");
		testDisplay("1 * 2 - 3");
		testDisplay("1 - 2 - 3");
		testDisplay("1 + 2 + 3");
		testDisplay("(1 + 2) * 3");
		testDisplay("a - b - c");
		testDisplay("a - (b - c)");
		testDisplay("(a, b) = (c, d)");
		Dump.details(Thread_.currentMethod(), Formatter.display(new Reference()));
	}

	@Test
	public void testDump() {
		testDump("-1");
		testDump("'-1'");
		testDump("'+xFEDC3210'");
	}

	private void testDisplay(String s) {
		var node = Suite.parse(s);
		assertEquals(s, Formatter.display(node));
	}

	private void testDump(String s) {
		var node = Suite.parse(s);
		assertEquals(s, Formatter.dump(node));
	}

}
