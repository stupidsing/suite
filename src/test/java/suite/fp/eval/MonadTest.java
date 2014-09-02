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
		StringWriter sw = new StringWriter();
		Node node = Suite.applyDo(Suite.parse("" //
				+ "define string v # \n" //
				+ "putm {atom:scope} {v} {\"abc\"} # \n" //
				+ "getm {atom:scope} {v} \n" //
				+ ""), Suite.parse("string"));
		Suite.evaluateFunToWriter(Suite.fcc(Suite.substitute("using MONAD >> .0", node), true), sw);
		assertEquals("abc", sw.toString());
	}

}
