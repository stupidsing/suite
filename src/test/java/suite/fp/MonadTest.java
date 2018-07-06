package suite.fp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;
import suite.util.Fail;

public class MonadTest {

	@Test
	public void testConcatm0() {
		assertEquals("abc", evalMonad("\"abc%0A\" | split_{10} | map_{sh_{\"cat\"}} | concatm"));
	}

	@Test
	public void testConcatm1() {
		assertEquals("abc\nabc\n", evalMonad("\"echo abc\" | iterate_{id} | take_{2} | map_{sh/_{}} | concatm"));
	}

	@Test
	public void testShell() {
		assertEquals("hello\n", evalMonad("sh_{\"echo hello\"}_{}"));
	}

	@Test
	public void testMutable() {
		var fp0 = "" //
				+ "do ( \n" //
				+ "    definem string v # \n" //
				+ "    v := \"abc\" # \n" //
				+ "    getm_{v} # \n" //
				+ ") \n";
		assertEquals("abc", evalMonad(fp0));
	}

	@Test
	public void testMutableFail() {
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

	private String evalMonad(String m) {
		return evalMonad(m, "string");
	}

	private String evalMonad(String m, String type) {
		return eval(Suite.applyPerform(Suite.parse(m), Suite.parse(type)));
	}

	private String eval(Node node) {
		var sw = new StringWriter();
		try (var sw_ = sw) {
			var node1 = Suite.substitute("use MONAD ~ .0", Suite.applyWriter(node));
			Suite.evaluateFunToWriter(Suite.fcc(node1, true), sw);
		} catch (IOException ex) {
			Fail.t(ex);
		}
		return sw.toString();
	}

}
