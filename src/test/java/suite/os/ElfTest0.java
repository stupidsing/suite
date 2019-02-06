package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.funp.Funp_;
import suite.ip.ImperativeCompiler;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest0 {

	private WriteElf elf = new WriteElf(Funp_.isAmd64);
	private ImperativeCompiler ic = new ImperativeCompiler();

	@Test
	public void test() {
		var program = "" //
				+ "declare inc = function [i0, out ix,] ( {ix} = i0 + 1; ); \n" //
				+ "signature j = int; \n" //
				+ "inc [41, out j,]; \n" //
				+ "j; \n" //
		;

		var exec = test(program, "");
		assertEquals(42, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testCat() {
		var program = "" //
				+ "declare linux-read = function [pointer:(byte * 256) buffer, int length,] ( \n" //
				+ "    buffer; \n" //
				+ "    asm _ MOV (ECX, EAX); \n" //
				+ "    length; \n" //
				+ "    asm _ MOV (EDX, EAX); \n" //
				+ "    asm _ MOV (EAX, 3); \n" //
				+ "    asm _ XOR (EBX, EBX); \n" //
				+ "    asm _ INT (+x80); \n" //
				+ "    -- length in EAX \n" //
				+ "); \n" //
				+ "\n" //
				+ "declare linux-write = function [pointer:(byte * 256) buffer, int length,] ( \n" //
				+ "    buffer; \n" //
				+ "    asm _ MOV (ECX, EAX); \n" //
				+ "    length; \n" //
				+ "    asm _ MOV (EDX, EAX); \n" //
				+ "    asm _ MOV (EAX, 4); \n" //
				+ "    asm _ MOV (EBX, 1); \n" //
				+ "    asm _ INT (+x80); \n" //
				+ "    -- length in EAX \n" //
				+ "); \n" //
				+ "\n" //
				+ "signature buffer = byte * 256; \n" //
				+ "declare nBytesRead; \n" //
				+ "\n" //
				+ "while (({nBytesRead} = linux-read [& buffer, 256,]) != 0) do ( \n" //
				+ "    linux-write [& buffer, nBytesRead,]; \n" //
				+ "); \n" //
				+ "0; \n" //
		;

		var text = "garbage\n";
		var exec = test(program, text);
		assertEquals(0, exec.code);
		assertEquals(text, exec.out);
	}

	private Execute test(String program, String text) {
		return elf.exec(text.getBytes(Defaults.charset), offset -> ic.compile(offset, "" //
				+ "asm _ MOV (EBP, ESP);" //
				+ program //
				+ "asm _ MOV (EBX, EAX);" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ INT (+x80);"));
	}

}
