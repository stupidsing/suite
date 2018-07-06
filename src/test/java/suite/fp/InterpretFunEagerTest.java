package suite.fp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class InterpretFunEagerTest {

	@Test
	public void testGroup() {
		expect("use source STANDARD ~ group_{1, 2; 2, 3; 1, 3;}", Suite.parse("1, (2; 3;); 2, (3;);"));
	}

	@Test
	public void testUsing() {
		expect("use source STANDARD ~ and_{true}_{true}", Atom.TRUE);
		expect("use source STANDARD ~ log_{1234}", Int.of(1234));
	}

	private void expect(String expr, Node expected) {
		assertEquals(expected, new InterpretFunEager().eager(Suite.parse(expr)));
	}

}
