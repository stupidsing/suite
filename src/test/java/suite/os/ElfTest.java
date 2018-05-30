package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Constants;
import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void testFold() {
		test("io (fold 0 (n => n < 100) (n => n + 1))", "", 100);
	}

	@Test
	public void testIterate() {
		test("iterate n 0 (n < 100) (io (n + 1))", "", 100);
	}

	// io :: a -> io a
	// io-cat :: io a -> (a -> io b) -> io b
	@Test
	public void testIo() {
		var text = "garbage\n";

		var program = "" //
				+ "expand size := 256 >> \n" //
				+ "let linux := consult \"linux.fp\" >> \n" //
				+ "iterate n 1 (n != 0) ( \n" //
				+ "	let buffer := size * array coerce-byte _ >> \n" //
				+ "	let pointer := address buffer >> \n" //
				+ "	pointer, size | linux/read | io-cat (nBytesRead => \n" //
				+ "		pointer, nBytesRead | linux/write | io-cat (nBytesWrote => \n" //
				+ "			io nBytesRead \n" //
				+ "		) \n" //
				+ "	) \n" //
				+ ") \n" //
		;

		test(program, text, 0);
	}

	private void test(String program, String input, int code) {
		var bytes = Bytes.of(input.getBytes(Constants.charset));
		var main = Funp_.main(true);

		if (RunUtil.isUnix()) { // not Windows => run ELF
			var exec = elf.exec(bytes.toArray(), offset -> main.compile(offset, program).t1);
			assertEquals(code, exec.code);
			assertEquals(input, exec.out);
		} else { // Windows => interpret assembly
			var pair = main.compile(code, program);
			var interpret = new Amd64Interpret();
			assertEquals(code, interpret.interpret(pair.t0, pair.t1, bytes));
			assertEquals(bytes, interpret.out.toBytes());
		}
	}

}
