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
	public void test() {
		Node node = Suite.parse("1 + 2 * 3");
		Funp f0 = new P0Parse().parse(node);
		Funp f1 = new P1InferType().infer(f0, new Reference());
		Bytes bytes = new P2GenerateCode().compile(f1, 0);
		System.out.println(bytes);
		assertTrue(bytes != null);
	}

}
