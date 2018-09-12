package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.assembler.Amd64Interpret;
import suite.cfg.Defaults;
import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private WriteElf elf = new WriteElf();

	@Test
	public void testFold() {
		test(100, "!for (n = 0; n < 100; n + 1)", "");
	}

	// io :: a -> io a
	// io.cat :: io a -> (a -> io b) -> io b
	@Test
	public void testIo() {
		var text = "garbage\n";

		var program = "" //
				+ "let linux := consult \"linux.fp\" ~ !do \n" //
				+ "	let !cat := linux/!cat ~ \n" //
				+ "	!cat {} ~ \n" //
				+ "	0 \n";

		test(0, program, text);
	}

	private void test(int code, String program, String input) {
		var bytes = Bytes.of(input.getBytes(Defaults.charset));
		var main = Funp_.main(true);

		if (Boolean.FALSE && RunUtil.isUnix()) { // not Windows => run ELF
			var exec = elf.exec(bytes.toArray(), offset -> main.compile(offset, program).t1);
			assertEquals(code, exec.code);
			assertEquals(input, exec.out);
		} else { // Windows => interpret assembly
			var pair = main.compile(Amd64Interpret.codeStart, program);
			var interpret = new Amd64Interpret();
			assertEquals(code, interpret.interpret(pair.t0, pair.t1, bytes));
			assertEquals(bytes, interpret.out.toBytes());
		}
	}

}
