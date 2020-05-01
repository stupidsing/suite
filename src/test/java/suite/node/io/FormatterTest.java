package suite.node.io;

import org.junit.jupiter.api.Test;
import primal.Adjectives.Current;
import suite.Suite;
import suite.inspect.Dump;
import suite.node.Int;
import suite.node.Reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		Dump.details(Current.method(), Formatter.display(new Reference()));
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
