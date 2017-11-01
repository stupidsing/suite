package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.funp.Funp_;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void test() {
		assertEquals("", test("0").out);
	}

	private Execute test(String program) {
		Execute exec = elf.exec("", offset -> Funp_.main().compile(offset, "" //
				+ "define linux-read := `buffer, length` => (\n" //
				+ "	type buffer = address (256 * array byte 0) >>\n" //
				+ "	type length = 0 >>\n" //
				+ "	0\n" //
				+ ") >>\n" //
				+ "asm {\n" //
				+ "	MOV (EBP, ESP);\n" //
				+ "} / ((" + program + ") | (i => asm {\n" //
				+ "	MOV (EBX, `EBP + 8`);\n" //
				+ "	MOV (EAX, 1);\n" //
				+ "	INT (-128);\n" //
				+ "}))"));
		assertEquals(0, exec.code);
		return exec;
	}

}
