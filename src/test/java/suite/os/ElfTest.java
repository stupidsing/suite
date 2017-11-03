package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.funp.Funp_;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void test() {
		assertEquals("", test("" //
				+ "define linux-read := `buffer, length` => (\n" //
				+ "	type buffer = address (256 * array byte _) >>\n" //
				+ "	type length = 0 >>\n" //
				+ "	asm (ECX = buffer; EDX = length;) {\n" //
				+ "		MOV (EAX, 3);\n" //
				+ "		XOR (EBX, EBX);\n" //
				+ "		INT (-128);\n" //
				+ "		-- length in EAX\n" //
				+ "	}\n" //
				+ ") >>\n" //
				+ "define linux-write := `buffer, length` => (\n" //
				+ "	type buffer = address (256 * array byte _) >>\n" //
				+ "	type length = 0 >>\n" //
				+ "	asm (ECX = buffer; EDX = length;) {\n" //
				+ "		MOV (EAX, 4);\n" //
				+ "		MOV (EBX, 1);\n" //
				+ "		INT (-128);\n" //
				+ "		-- length in EAX\n" //
				+ "	}\n" //
				+ ") >>\n" //
				+ "iterate v 0 (v < 100) (v + 1)\n" //
		).out);
	}

	private Execute test(String program) {
		Execute exec = elf.exec("", offset -> Funp_.main().compile(offset, "" //
				+ "asm () {\n" //
				+ "	MOV (EBP, ESP);\n" //
				+ "} / ((\n" //
				+ program + "\n" //
				+ ") | (i => asm () {\n" //
				+ "	MOV (EBX, `EBP + 8`);\n" //
				+ "	MOV (EAX, 1);\n" //
				+ "	INT (-128);\n" //
				+ "}))\n" //
		));
		assertEquals(0, exec.code);
		return exec;
	}

}
