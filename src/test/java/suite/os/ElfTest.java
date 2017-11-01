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
				+ "define linux-read := i => (type i = 0 >> 0) >>\n" //
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
