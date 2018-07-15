import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class MoliuTest {

	@Test
	public void testLambda() {
		test(3, "1, 2 | ((a, b) => 3)");
	}

	private void test(int r, String p) {
		for (var isOptimize : new boolean[] { false, }) {
			LogUtil.info(p);
			var pair = Funp_.main(isOptimize).compile(Amd64Interpret.codeStart, p);
			var bytes = pair.t1;
			LogUtil.info("Hex" + bytes + "\n\n");
			assertEquals(r, new Amd64Interpret().interpret(pair.t0, Bytes.of(), Bytes.of()));
			assertTrue(bytes != null);
		}
	}

}
