package suite.fp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class MonadTest {

	@Before
	public void before() {
		Suite.libraries = Arrays.asList("STANDARD", "CHARS", "TEXT");
	}

	@Test
	public void testConcatm0() throws IOException {
		assertEquals("abc", evalMonad("\"abc%0A\" | split {10} | map {sh {\"cat\"}} | concatm"));
	}

	@Test
	public void testConcatm1() throws IOException {
		assertEquals("abc\nabc\n", evalMonad("\"echo abc\" | iterate {id} | take {2} | map {sh/ {}} | concatm"));
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
			assertEquals("abc", evalMonad("" //
					+ "do >> \n" //
					+ "    definem int v # \n" //
					+ "    v := \"abc\" # \n" //
					+ "", "any"));
		} catch (RuntimeException ex) {
			// Unmatched types
		}
	}

	private String evalMonad(String m) throws IOException {
		return evalMonad(m, "string");
	}

	private String evalMonad(String m, String type) throws IOException {
		return eval(Suite.applyPerform(Suite.parse(m), Suite.parse(type)));
	}

	private String eval(Node node) throws IOException {
		StringWriter sw = new StringWriter();
		Node node1 = Suite.substitute("using MONAD >> .0", Suite.applyWriter(node));
		Suite.evaluateFunToWriter(Suite.fcc(node1, true), sw);
		return sw.toString();
	}

}
