package suite.funp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.primitive.Bytes;

public class CrudeScriptTest {

	@Test
	public void test() {
		var p4 = new P4GenerateCode(true);
		var isOptimize = false;

		var f0 = new P0CrudeScript().parse("{ return 1 + 2 * 3; }");
		var f1 = new P1Inline().inline(f0, isOptimize ? 3 : 0, 1, 1, 1, 1, 1);
		var f2 = new P2InferType().infer(f1);
		var f3 = new P3Optimize().optimize(f2);
		var pair = p4.compile(Amd64Interpret.codeStart, f3);
		assertEquals(7, new Amd64Interpret().interpret(pair, Bytes.of()));
	}

}
