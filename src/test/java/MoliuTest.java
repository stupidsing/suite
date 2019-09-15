import static org.junit.Assert.assertEquals;

import org.junit.Test;

import primal.os.Log_;
import primal.primitive.adt.Bytes;
import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;

public class MoliuTest {

	private Amd64Interpret interpret = new Amd64Interpret();

	@Test
	public void testLambda() {
		test(6, "define.function f a :=  a + 1 ~ 3 | f | f | f");
	}

	private void test(int expected, String program) {
		for (var isOptimize : new boolean[] { false, }) {
			Log_.info(program);

			var actual = Funp_ //
					.main(isOptimize) //
					.compile(interpret.codeStart, program) //
					.map((instructions, code) -> {
						Log_.info("Hex" + code + "\n\n");
						return interpret.interpret(instructions, code, Bytes.of());
					}) //
					.intValue();

			assertEquals(expected, actual);
		}
	}

}
