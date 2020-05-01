package suite.fp;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.node.Atom;
import suite.node.Node;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
