package suite.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import primal.Nouns.Tmp;
import primal.Nouns.Utf8;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.pair.IntObjPair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Interpret;
import suite.assembler.Amd64Mode;
import suite.funp.Funp_;
import suite.util.RunUtil;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private boolean isLongMode = RunUtil.isLinux64();

	private Amd64 amd64 = Amd64.me;
	private Amd64Interpret interpret = new Amd64Interpret();
	private WriteElf elf = new WriteElf(isLongMode);

	@Test
	public void testAllocate() {
		test(0, "" //
				+ "let { !alloc, !dealloc, } := consult allocate.fp ~ do! ( \n" //
				+ "let p := !alloc 12 ~ \n" //
				+ "let q := !alloc 24 ~ \n" //
				+ "!dealloc (12, p) ~ \n" //
				+ "!dealloc (24, q) ~ \n" //
				+ "0 \n" //
				+ ") \n" //
				, "");
	}

	@Test
	public void testAssembler() {
		List<Instruction> instructions;

		if (isLongMode)
			instructions = List.of( //
					amd64.instruction(Insn.MOV, amd64.rax, amd64.imm64(0x3C)), //
					amd64.instruction(Insn.MOV, amd64.rdi, amd64.imm64(0x00)), //
					amd64.instruction(Insn.SYSCALL));
		else
			instructions = List.of( //
					amd64.instruction(Insn.MOV, amd64.eax, amd64.imm32(0x01)), //
					amd64.instruction(Insn.INT, amd64.imm8(0x80)));

		var aa = new Amd64Assemble(isLongMode ? Amd64Mode.LONG64 : Amd64Mode.PROT32);
		var exec = elf.exec(new byte[0], offset -> aa.assemble(offset, instructions, true));
		assertEquals(0, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testCall() {
		test(3, "let id := (i => i + 1) ~ 2 | id", "");
	}

	@Test
	public void testCapture() {
		test(46, "define m := 31 ~ let l := n => capture1 (n + m) ~ 15 | l", "");
		test(46, "define m := 31 ~ let l := n => capture (n + m) ~ uncapture l ~ 15 | l", "");
	}

	@Test
	public void testFold() {
		test(100, "fold (n := 0 # n < 100 # n + 1 # n)", "");
	}

	@Test
	public void testGuess() {
		var program = "do! (consult guess.fp)/!guess {}";
		elf.write(offset -> Funp_.main(isLongMode, false).compile(offset, program).v, Tmp.path("guess"));
		elf.write(offset -> Funp_.main(isLongMode, true).compile(offset, program).v, Tmp.path("guess"));
	}

	// io :: a -> io a
	// io.cat :: io a -> (a -> io b) -> io b
	@Test
	public void testIo() {
		var text = "garbage\n";
		var program = "do! (consult io.fp)/!cat {}";
		test(0, program, text);
	}

	@Test
	public void testIter() {
		// var program = "do! (consult iter.fp)/!list.iter []";
		var program = "" //
				+ "let { list.filter, !list.free, !list.iter, list.map, } := consult iter.fp ~ \n" //
				+ "do! ( \n" //
				+ "	let !filter := list.filter (i => true) ~ \n" //
				+ "	let !map := list.map (i => i + 1) ~ \n" //
				+ "	let list0 := { elems: address.of predef (array 32 * 1), size: 3, } ~ \n" //
				+ "	let list1 := !filter list0 ~ \n" //
				+ "	let list2 := !map list1 ~ \n" //
				+ "	let iter := !list.iter list2 ~ \n" //
				+ "	let v := iter/!next () ~ \n" //
				+ "	iter/!free () ~ \n" //
				+ "	!list.free list2 ~ \n" //
				+ "	!list.free list1 ~ \n" //
				+ "	uncapture !filter ~ \n" //
				+ "	uncapture !map ~ \n" //
				+ "	v \n" //
				+ ")";
		System.out.println(program);
		test(2, program, "");
	}

	@Test
	public void testNumbers() {
		test(0, "" //
				+ "let { !get.number, !put.number, } := consult io.fp ~ \n" //
				+ "do! ( \n" //
				+ "let m := type number !get.number {} ~ \n" //
				+ "let n := type number !get.number {} ~ \n" //
				+ "!put.number (m + n) ~ 0 \n" //
				+ ")", //
				"25\n57\n", //
				"82");
	}

	@Test
	public void testPut() {
		test(0, "do! (!! (consult io.fp)/!put.char byte 'A' ~ 0)", "A");
		test(0, "do! (!! (consult io.fp)/!put.number number 'A' ~ 0)", "65");
		test(0, "do! (!! (consult io.fp)/!put.number -999 ~ 0)", "-999");
		test(0, "for! (i := 0 # i < 10 # !! (consult io.fp)/!put.number i ~ i + 1 # 0)", "0123456789");
	}

	@Test
	public void testRdtsc() {
		execute("consult 'asm.${platform}.fp' ~ do! (!asm.rdtsc and +x7FFFFFFF % 100)", "");
		execute("consult 'asm.${platform}.fp' ~ do! (!asm.rdtscp and +x7FFFFFFF % 100)", "");
	}

	@Test
	public void testZero() {
		test(0, "0", "");
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
		var main = Funp_.main(isLongMode, true);
		var result = IntObjPair.of(-1, "-");

		if (Boolean.TRUE && RunUtil.isLinux()) { // not Windows => run ELF
			var exec = elf.exec(ibs.toArray(), offset -> main.compile(offset, program).v);
			result.update(exec.code, exec.out);
		} else { // Windows => interpret assembly
			var pair = main.compile(interpret.codeStart, program);
			result.update(interpret.interpret(pair, ibs), new String(interpret.out.toBytes().toArray(), Utf8.charset));
		}

		return result;
	}

}
