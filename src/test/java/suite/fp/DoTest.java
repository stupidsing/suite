package suite.fp;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;

public class DoTest {

	@Before
	public void before() {
		Suite.libraries = Arrays.asList("STANDARD", "CHARS", "TEXT");
	}

	@Test
	public void test() throws IOException {
		Node node = Suite.applyPerform(Suite.parse("sh {\"git status\"} {}"), Atom.of("any"));
		StringWriter writer = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(Suite.applyWriter(node)), writer);
		String out = writer.toString();
		System.out.println(out);
		assertNotNull(out);
	}

}
