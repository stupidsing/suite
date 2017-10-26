package suite.os;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.junit.Test;

import suite.ip.ImperativeCompiler;
import suite.primitive.Bytes;
import suite.util.TempDir;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest0 {

	@Test
	public void test() {
		String program = "" //
				+ "declare inc = function [i0, out ix,] ( {ix} = i0 + 1; ); \n" //
				+ "signature j = int; \n" //
				+ "inc [41, out j,]; \n" //
				+ "j; \n" //
		;

		compileElf(program);
	}

	@Test
	public void testCat() {
		String program = "" //
				+ "declare linux-read = function [pointer:(byte * 256) buffer, int length,] ( \n" //
				+ "    buffer; \n" //
				+ "    asm _ MOV (ECX, EAX); \n" //
				+ "    length; \n" //
				+ "    asm _ MOV (EDX, EAX); \n" //
				+ "    asm _ MOV (EAX, 3); \n" //
				+ "    asm _ XOR (EBX, EBX); \n" //
				+ "    asm _ INT (-128); \n" //
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
				+ "    asm _ INT (-128); \n" //
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

		String text = "garbage\n";
		Path path = compileElf(program);
		assertEquals(text, exec(text, path).out);
	}

	private Path compileElf(String program) {
		String program1 = "" //
				+ "asm _ MOV (EBP, ESP);" //
				+ program //
				+ "asm _ MOV (EBX, EAX);" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ INT (-128);";

		int org = 0x08048000;

		Bytes code = new ImperativeCompiler().compile(org + 84, program1);
		Path path = TempDir.resolve("a.out");
		new ElfWriter().write(org, code, path);
		return path;
	}

	private Execute exec(String text, Path path) {
		Execute exec = new Execute(new String[] { path.toString(), }, text);
		assertEquals(0, exec.code);
		return exec;
	}

}
