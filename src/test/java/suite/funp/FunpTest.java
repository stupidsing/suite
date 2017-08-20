package suite.funp;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.assembler.Amd64.Instruction;
import suite.funp.Funp_.Funp;
import suite.node.Node;
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
		P0Parse p0 = new P0Parse();
		P1InferType p1 = new P1InferType();
		P2GenerateCode p2 = new P2GenerateCode();

		Funp f0 = p0.parse(node);
		Funp f1 = p1.infer(f0);
		List<Instruction> instructions = p2.compile0(f1);
		return p2.compile1(0, instructions, true);
	}

}
