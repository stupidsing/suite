package suite.os;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.Constants;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void testCode() {
		test("iterate n 0 (n < 100) (io (n + 1)) \n", "", 100);
	}

	@Test
	public void testIo() {
		String text = "garbage\n";

		String program = "" //
				+ "expand size := 256 >> \n" //
				+ "define linux-mmap := `length` => ( \n" //
				+ "	let ps := array (0, length, 3, 34, -1, 0,) >> \n" //
				+ "	asm (EAX = 90; EBX = address ps;) { \n" //
				+ "		INT (-128); \n" //
				+ "	} \n" //
				+ ") >> \n" //
				+ "define linux-munmap := `pointer, length` => ( \n" //
				+ "	type pointer = address (size * array byte _) >> \n" //
				+ "	asm (EAX = 91; EBX = pointer; ECX = length;) { \n" //
				+ "		INT (-128); \n" //
				+ "	} \n" //
				+ ") >> \n" //
				+ "define linux-read := `pointer, length` => ( \n" //
				+ "	type pointer = address (size * array byte _) >> \n" //
				+ "	asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { \n" //
				+ "		INT (-128); -- length in EAX \n" //
				+ "	} \n" //
				+ ") >> \n" //
				+ "define linux-write := `pointer, length` => ( \n" //
				+ "	type pointer = address (size * array byte _) >> \n" //
				+ "	asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { \n" //
				+ "		INT (-128); -- length in EAX \n" //
				+ "	} \n" //
				+ ") >> \n" //
				+ "iterate n 1 (n != 0) ( \n" //
				+ "	let buffer := (size * array byte _) >> \n" //
				+ "	let pointer := address buffer >> \n" //
				+ "	pointer, size | linux-read | io-cat ( \n" //
				+ "		nBytesRead => pointer, nBytesRead | linux-write | io-cat (nBytesWrote => io nBytesRead) \n" //
				+ "	) \n" //
				+ ") \n" //
		;

		test(program, text, 0);
	}

	private void test(String program, String input, int code) {
		Bytes bytes = Bytes.of(input.getBytes(Constants.charset));

		if (RunUtil.isUnix()) { // not Windows => run ELF
			Execute exec = elf.exec(bytes.toArray(), offset -> Funp_.main().compile(offset, program).t1);
			assertEquals(code, exec.code);
			assertEquals(input, exec.out);
		} else { // Windows => interpret assembly
			List<Instruction> instructions = Funp_.main().compile(code, program).t0;

			Amd64Interpret interpret = new Amd64Interpret(bytes);
			assertEquals(code, interpret.interpret(instructions));
			assertEquals(bytes, interpret.out.toBytes());
		}
	}

}
