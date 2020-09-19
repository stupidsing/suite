package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static primal.statics.Fail.fail;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.node.Node;

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
		var fp0 = """
				do (
				    definem string v #
				    v := \"abc\" #
				    getm_{v} #
				)
				""";
		assertEquals("abc", evalMonad(fp0));
	}

	@Test
	public void testMutableFail() {
		var fp0 = """
				do (
				    definem int v #
				    v := "abc" #
				)
				""";

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
			fail(ex);
		}
		return sw.toString();
	}

}
