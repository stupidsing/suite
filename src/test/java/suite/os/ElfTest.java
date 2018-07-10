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
		test(100, "io.fold 0 (n => n < 100) (n => io (n + 1))", "");
	}

	// io :: a -> io a
	// io.cat :: io a -> (a -> io b) -> io b
	@Test
	public void testIo() {
		var text = "garbage\n";

		var program = "" //
				+ "let linux := consult \"linux.fp\" ~ \n" //
				+ "io.perform linux/cat ~ \n" //
				+ "io 0 \n";

		test(0, program, text);
	}

	private void test(int code, String program, String input) {
		var bytes = Bytes.of(input.getBytes(Defaults.charset));
		var main = Funp_.main(true);

		if (RunUtil.isUnix()) { // not Windows => run ELF
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
