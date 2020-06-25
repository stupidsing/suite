import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import primal.Nouns.Utf8;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.pair.IntObjPair;
import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;
import suite.util.RunUtil;

public class MoliuTest {

	private boolean isLongMode = RunUtil.isLinux64();

	private Amd64Interpret interpret = new Amd64Interpret();

	@Test
	public void testIter() throws IOException {
		// var program = "do! (consult iter.fp)/!list.iter []";
		var program = Files.readString(Paths.get("src/main/resources/suite/funp/iter_test.fp"));
		test(2, program, "", "");
	}

	private void test(int code, String program, String expected) {
		test(code, program, expected, expected);
	}

	private void test(int code, String program, String input, String expected) {
		var result = execute(program, input);
		assertEquals(code, result.k);
		assertEquals(expected, result.v);
	}

	private IntObjPair<String> execute(String program, String input) {
		var ibs = Bytes.of(input.getBytes(Utf8.charset));
		var main = Funp_.main(isLongMode, false);
		var result = IntObjPair.of(-1, "-");
		var pair = main.compile(interpret.codeStart, program);
		result.update(interpret.interpret(pair, ibs), new String(interpret.out.toBytes().toArray(), Utf8.charset));
		return result;
	}

}
