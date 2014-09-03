package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class MonadTest {

	@Test
	public void testMonad() throws IOException {
		assertEquals("abc", eval(Suite.applyDo(Suite.parse("" //
				+ "do >> \n" //
				+ "    definem string v # \n" //
				+ "    putm {v} {\"abc\"} # \n" //
				+ "    getm {v} # \n" //
				+ ""), Suite.parse("string"))));
	}

	@Test
	public void testMonadFail() throws IOException {
		try {
			assertEquals("abc", eval(Suite.applyDo(Suite.parse("" //
					+ "do >> \n" //
					+ "    definem int v # \n" //
					+ "    putm {v} {\"abc\"} # \n" //
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
