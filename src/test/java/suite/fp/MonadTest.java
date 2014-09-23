package suite.fp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class MonadTest {

	@Test
	public void testConcatm() throws IOException {
		List<String> libraries0 = Suite.libraries;
		Suite.libraries = Arrays.asList("STANDARD", "MONAD");
		try {
			assertEquals("abc\n", evalMonad("\"abc%0A\" | split {10} | map {sh {\"cat\"}} | concatm"));
		} finally {
			Suite.libraries = libraries0;
		}
	}

	@Test
	public void testShell() throws IOException {
		assertEquals("hello\n", evalMonad("sh {\"echo hello\"} {}"));
	}

	@Test
	public void testMutable() throws IOException {
		assertEquals("abc", evalMonad("" //
				+ "do >> \n" //
				+ "    definem string v # \n" //
				+ "    v := \"abc\" # \n" //
				+ "    getm {v} # \n" //
				+ ""));
	}

	@Test
	public void testMutableFail() throws IOException {
		try {
			assertEquals("abc", eval(Suite.applyDo(Suite.parse("" //
					+ "do >> \n" //
					+ "    definem int v # \n" //
					+ "    v := \"abc\" # \n" //
					+ ""), Suite.parse("any"))));
		} catch (RuntimeException ex) {
			// Unmatched types
		}
	}

	private String evalMonad(String m) throws IOException {
		return eval(Suite.applyDo(Suite.parse(m), Suite.parse("string")));
	}

	private String eval(Node node) throws IOException {
		StringWriter sw = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(Suite.substitute("using MONAD >> .0", node), true), sw);
		return sw.toString();
	}

}
