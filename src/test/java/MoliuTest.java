import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.cfg.Defaults;
import suite.funp.Funp_;
import suite.primitive.Bytes;

public class MoliuTest {

	@Test
	public void testIo() {
		var text = "";

		var program = "" //
				+ "let f := 0 ~ " //
				+ "let g {} := { ref {} := f ~ } ~ " //
				+ "0";

		test(0, program, text);
	}

	private void test(int code, String program, String input) {
		var bytes = Bytes.of(input.getBytes(Defaults.charset));
		var main = Funp_.main(false);

		{ // Windows => interpret assembly
			var pair = main.compile(Amd64Interpret.codeStart, program);
			var interpret = new Amd64Interpret();
			assertEquals(code, interpret.interpret(pair.t0, pair.t1, bytes));
			assertEquals(bytes, interpret.out.toBytes());
		}
	}

}
