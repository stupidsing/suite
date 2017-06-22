package suite.funp;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.funp.Funp_.Funp;
import suite.node.Node;
import suite.node.Reference;
import suite.primitive.Bytes;

public class FunpTest {

	@Test
	public void testExpr() {
		Node node = Suite.parse("1 + 2 * 3");
		Bytes bytes = compile(node);
		System.out.println(bytes);
		assertTrue(bytes != null);
	}

	@Test
	public void testLambda() {
		Node node = Suite.parse("0 | (a => a + 1)");
		Bytes bytes = compile(node);
		System.out.println(bytes);
		assertTrue(bytes != null);
	}

	private Bytes compile(Node node) {
		Funp f0 = new P0Parse().parse(node);
		Funp f1 = new P1InferType().infer(f0, new Reference());
		return new P2GenerateCode().compile(f1, 0);
	}

}
