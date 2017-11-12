package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.funp.Funp_;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void testCode() {
		Execute exec = test("" //
				+ "iterate n 0 (n < 100) (n + 1)\n" //
				, "");

		assertEquals(100, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testIo() {
		String text = "garbage\n";

		Execute exec = test("" //
				+ "expand size := 256 >>\n" //
				+ "define linux-read := `buffer, length` => (\n" //
				+ "	type buffer = address (size * array byte _) >>\n" //
				+ "	asm (EAX = 3; EBX = 0; ECX = buffer; EDX = length;) {\n" //
				+ "		INT (-128);\n" //
				+ "		-- length in EAX\n" //
				+ "	}\n" //
				+ ") >>\n" //
				+ "define linux-write := `buffer, length` => (\n" //
				+ "	type buffer = address (size * array byte _) >>\n" //
				+ "	asm (EAX = 4; EBX = 1; ECX = buffer; EDX = length;) {\n" //
				+ "		INT (-128);\n" //
				+ "		-- length in EAX\n" //
				+ "	}\n" //
				+ ") >>\n" //
				+ "iterate n 1 (n != 0) (\n" //
				+ "	let buffer := (size * array byte _) >>\n" //
				+ "	let nBytesRead := (address buffer, size | linux-read) >> (\n" //
				+ "		(address buffer, nBytesRead | linux-write);\n" //
				+ "		nBytesRead" //
				+ "	)\n" //
				+ ")\n" //
				, text);

		assertEquals(0, exec.code);
		assertEquals(text, exec.out);
	}

	private Execute test(String program, String input) {
		return elf.exec(input, offset -> Funp_.main().compile(offset, program));
	}

}
