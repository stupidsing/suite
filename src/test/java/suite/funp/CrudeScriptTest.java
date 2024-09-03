package suite.funp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import primal.primitive.adt.Bytes;
import suite.assembler.Amd64Interpret;
import suite.funp.p0.P0CrudeScript;
import suite.funp.p1.P12Inline;
import suite.funp.p2.P22InferType;
import suite.funp.p3.P3Optimize;
import suite.funp.p4.P4GenerateCode;

public class CrudeScriptTest {

	@Test
	public void test() {
		for (var isLongMode : new boolean[] { false, true, }) {
			var interpret = new Amd64Interpret(isLongMode);
			var f = new Funp_(isLongMode, false);
			var isOptimize = false;

			var f0 = new P0CrudeScript(f).parse("{ return 1 + 2 * 3; }");
			var f1 = new P12Inline(f).inline(f0, isOptimize ? 3 : 0);
			var f2 = new P22InferType(f).infer(f1);
			var f3 = new P3Optimize(f).optimize(f2);
			var f4 = new P4GenerateCode(f).compile(interpret.codeStart, f3);
			var pair = f4;

			assertEquals(7, interpret.interpret(pair, Bytes.of()));
		}
	}

}
