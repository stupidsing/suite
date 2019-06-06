package suite.os;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Interpret;
import suite.cfg.Defaults;
import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private boolean isAmd64 = Funp_.isAmd64;

	private Amd64 amd64 = Amd64.me;
	private Amd64Interpret interpret = new Amd64Interpret();
	private WriteElf elf = new WriteElf(isAmd64);

	@Test
	public void testAssembler() {
		List<Instruction> instructions;

		if (isAmd64)
			instructions = List.of( //
					amd64.instruction(Insn.MOV, amd64.rax, amd64.imm64(0x3C)), //
					amd64.instruction(Insn.MOV, amd64.rdi, amd64.imm64(0x00)), //
					amd64.instruction(Insn.SYSCALL));
		else
			instructions = List.of( //
					amd64.instruction(Insn.MOV, amd64.eax, amd64.imm(0x01, 4)), //
					amd64.instruction(Insn.INT, amd64.imm8(0x80)));

		var aa = new Amd64Assemble(Funp_.pushSize);
		var exec = elf.exec(new byte[0], offset -> aa.assemble(offset, instructions, true));
		assertEquals(0, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testCall() {
		test(3, "let id := (i => i + 1) ~ (2 | id)", "");
	}

	@Test
	public void testFold() {
		test(100, "fold (n := 0 # n < 100 # n + 1)", "");
	}

	// io :: a -> io a
	// io.cat :: io a -> (a -> io b) -> io b
	@Test
	public void testIo() {
		var text = "garbage\n";

		var program = "" //
				+ "let linux := consult \"linux.fp\" ~ do! \n" //
				+ "	let !cat := linux/!cat ~ \n" //
				+ "	!cat {} ~ \n" //
				+ "	0 \n";

		test(0, program, text);
	}

	@Test
	public void testPutChar() {
		test(0, "let !put.char := (consult \"linux.fp\")/!put.char ~ do! (!put.char byte 65 ~ 0)", "A");
		test(0, "let !put.number := (consult \"linux.fp\")/!put.number ~ do! (!put.number 65 ~ 0)", "65");
		test(0, "let !put.number := (consult \"linux.fp\")/!put.number ~ do! (!put.number -999 ~ 0)", "-999");
		test(9, "let !put.number := (consult \"linux.fp\")/!put.number ~ for! (i := 0 # i < 10 # !put.number i ~ i + 1)", "0123456789");
	}

	@Test
	public void testZero() {
		test(0, "0", "");
	}

	private void test(int code, String program, String expected) {
		var input = Bytes.of(expected.getBytes(Defaults.charset));
		var main = Funp_.main(true);

		if (Boolean.TRUE && RunUtil.isLinux()) { // not Windows => run ELF
			var exec = elf.exec(input.toArray(), offset -> main.compile(offset, program).t1);
			assertEquals(code, exec.code);
			assertEquals(expected, exec.out);
		} else { // Windows => interpret assembly
			var pair = main.compile(interpret.codeStart, program);
			assertEquals(code, interpret.interpret(pair, input));
			assertEquals(input, interpret.out.toBytes());
		}
	}

}
