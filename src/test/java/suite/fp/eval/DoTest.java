package suite.fp.eval;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;

public class DoTest {

	@Test
	public void test() throws IOException {
		Node node = Suite.applyDo(Suite.parse("sh {\"git status\"} {}"), Atom.of("any"));
		StringWriter writer = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(node), writer);
		String out = writer.toString();
		System.out.println(out);
		assertNotNull(out);
	}

}
