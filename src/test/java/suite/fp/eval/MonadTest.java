package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class MonadTest {

	@Test
	public void testShell() throws IOException {
		assertEquals("hello\n", eval(Suite.applyDo(Suite.parse("" //
				+ "sh {\"echo hello\"} {}"), Suite.parse("string"))));
	}

	@Test
	public void testMutable() throws IOException {
		assertEquals("abc", eval(Suite.applyDo(Suite.parse("" //
				+ "do >> \n" //
				+ "    definem string v # \n" //
				+ "    setm v := \"abc\" # \n" //
				+ "    getm {v} # \n" //
				+ ""), Suite.parse("string"))));
	}

	@Test
	public void testMutableFail() throws IOException {
		try {
			assertEquals("abc", eval(Suite.applyDo(Suite.parse("" //
					+ "do >> \n" //
					+ "    definem int v # \n" //
					+ "    setm v := \"abc\" # \n" //
					+ ""), Suite.parse("any"))));
		} catch (RuntimeException ex) {
			// Unmatched types
		}
	}

	private String eval(Node node) throws IOException {
		StringWriter sw = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(Suite.substitute("using MONAD >> .0", node), true), sw);
		return sw.toString();
	}

}
