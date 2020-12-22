package suite.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import primal.Nouns.Utf8;
import suite.ip.ImperativeCompiler;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest0 {

	private WriteElf elf = new WriteElf(false);
	private ImperativeCompiler ic = new ImperativeCompiler();

	@Test
	public void test() {
		var program = """
				declare inc = function [i0, out ix,] ( {ix} = i0 + 1; );
				signature j = int;
				inc [41, out j,];
				j;
				""";

		var exec = test(program, "");
		assertEquals(42, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testCat() {
		var program = """
				declare linux-read = function [pointer:(byte * 256) buffer, int length,] (
					buffer;
					asm _ MOV (ECX, EAX);
					length;
					asm _ MOV (EDX, EAX);
					asm _ MOV (EAX, 3);
					asm _ XOR (EBX, EBX);
					asm _ INT (-128);
					-- length in EAX
				);

				declare linux-write = function [pointer:(byte * 256) buffer, int length,] (
					buffer;
					asm _ MOV (ECX, EAX);
					length;
					asm _ MOV (EDX, EAX);
					asm _ MOV (EAX, 4);
					asm _ MOV (EBX, 1);
					asm _ INT (-128);
					-- length in EAX
				);

				signature buffer = byte * 256;
				declare nBytesRead;

				while (({nBytesRead} = linux-read [& buffer, 256,]) != 0) do (
					linux-write [& buffer, nBytesRead,];
				);
				0;
				""";

		var text = "garbage\n";
		var exec = test(program, text);
		assertEquals(0, exec.code);
		assertEquals(text, exec.out);
	}

	private Execute test(String program, String text) {
		return elf.exec(text.getBytes(Utf8.charset), offset -> ic.compile(offset, """
				asm _ MOV (EBP, ESP);
				(""" + program + """
				);
				asm _ MOV (EBX, EAX);
				asm _ MOV (EAX, 1);
				asm _ INT (-128);
				"""));
	}

}
