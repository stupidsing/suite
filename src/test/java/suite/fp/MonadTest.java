package suite.fp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class MonadTest {

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
		var fp0 = "" //
				+ "do ( \n" //
				+ "    definem string v # \n" //
				+ "    v := \"abc\" # \n" //
				+ "    getm {v} # \n" //
				+ ") \n";
		assertEquals("abc", evalMonad(fp0));
	}

	@Test
	public void testMutableFail() throws IOException {
		var fp0 = "" //
				+ "do ( \n" //
				+ "    definem int v # \n" //
				+ "    v := \"abc\" # \n" //
				+ ") \n";

		try {
			assertEquals("abc", evalMonad(fp0, "any"));
		} catch (RuntimeException ex) {
			// unmatched types
		}
	}

	private String evalMonad(String m) throws IOException {
		return evalMonad(m, "string");
	}

	private String evalMonad(String m, String type) throws IOException {
		return eval(Suite.applyPerform(Suite.parse(m), Suite.parse(type)));
	}

	private String eval(Node node) throws IOException {
		var sw = new StringWriter();
		var node1 = Suite.substitute("use MONAD ~ .0", Suite.applyWriter(node));
		Suite.evaluateFunToWriter(Suite.fcc(node1, true), sw);
		return sw.toString();
	}

}
