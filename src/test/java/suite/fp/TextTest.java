package suite.fp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;

public class TextTest {

	@Test
	public void testCamelCase() {
		assertEquals(eval("\"Text\""), eval("camel-case_{\"text\"}"));
	}

	@Test
	public void testSh() throws IOException {
		var node = Suite.applyPerform(Suite.parse("sh_{\"git status\"}_{}"), Atom.of("any"));
		var writer = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(Suite.applyWriter(node)), writer);
		var out = writer.toString();
		System.out.println(out);
		assertNotNull(out);
	}

	private static Node eval(String f) {
		return Suite.evaluateFun(f, true);
	}

}
