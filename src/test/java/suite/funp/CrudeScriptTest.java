package suite.funp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import primal.primitive.adt.Bytes;
import suite.assembler.Amd64Interpret;
import suite.funp.p1.P10Inline;
import suite.funp.p2.P2InferType;
import suite.funp.p3.P3Optimize;
import suite.funp.p4.P4GenerateCode;

public class CrudeScriptTest {

	private Amd64Interpret interpret = new Amd64Interpret();

	@Test
	public void test() {
		var p4 = new P4GenerateCode(true);
		var isOptimize = false;

		var f0 = new P0CrudeScript().parse("{ return 1 + 2 * 3; }");
		var f1 = new P10Inline().inline(f0, isOptimize ? 3 : 0, 1, 1, 1, 1, 1);
		var f2 = new P2InferType().infer(f1);
		var f3 = new P3Optimize().optimize(f2);
		var pair = p4.compile(interpret.codeStart, f3);
		assertEquals(7, interpret.interpret(pair, Bytes.of()));
	}

}
