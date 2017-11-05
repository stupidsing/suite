package suite.os;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.funp.Funp_;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void test() {
		Execute exec = test("" //
				+ "define id := i => i + 1 >>\n" //
				+ "iterate v 0 (v < 100) (v + 1)\n");
		assertEquals(100, exec.code);
		assertEquals("", exec.out);
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
		return exec;
	}

}
