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
				+ "asm {" //
				+ "	MOV (EBP, ESP);" //
				+ "} / ((" + program + ") | (i => asm {" //
				+ "	MOV (EBX, `EBP + 8`);" //
				+ "	MOV (EAX, 1);" //
				+ "	INT (-128);" //
				+ "}))"));
		assertEquals(0, exec.code);
		return exec;
	}

}
