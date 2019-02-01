package suite.os;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Interpret;
import suite.cfg.Defaults;
import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private Amd64 amd64 = Amd64.me;
	private Amd64Interpret interpret = new Amd64Interpret();
	private WriteElf elf = new WriteElf();

	@Test
	public void testAsm() {
		var instructions = List.of( //
				amd64.instruction(Insn.MOV, amd64.rax, amd64.imm(0x3C, 8)), //
				amd64.instruction(Insn.MOV, amd64.rdi, amd64.imm(0x00, 8)), //
				amd64.instruction(Insn.SYSCALL));

		var exec = elf.exec(new byte[0], offset -> new Amd64Assemble().assemble(offset + 84, instructions, true));
		assertEquals(0, exec.code);
		assertEquals("", exec.out);
	}

//	@Test
	public void testFold() {
		test(100, "!for (n = 0; n < 100; n + 1)", "");
	}

	// io :: a -> io a
	// io.cat :: io a -> (a -> io b) -> io b
//	@Test
	public void testIo() {
		var text = "garbage\n";

		var program = "" //
				+ "let linux := consult \"linux.fp\" ~ !do \n" //
				+ "	let !cat := linux/!cat ~ \n" //
				+ "	!cat {} ~ \n" //
				+ "	0 \n";

		test(0, program, text);
	}

	private void test(int code, String program, String in) {
		var input = Bytes.of(in.getBytes(Defaults.charset));
		var main = Funp_.main(true);

		if (Boolean.TRUE && RunUtil.isUnix()) { // not Windows => run ELF
			var exec = elf.exec(input.toArray(), offset -> main.compile(offset, program).t1);
			assertEquals(code, exec.code);
			assertEquals(in, exec.out);
		} else { // Windows => interpret assembly
			var pair = main.compile(interpret.codeStart, program);
			assertEquals(code, interpret.interpret(pair, input));
			assertEquals(input, interpret.out.toBytes());
		}
	}

}
