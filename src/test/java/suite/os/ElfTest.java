package suite.os;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.junit.Test;

import suite.funp.Funp_;
import suite.util.TempDir;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void test() {
		Path path = compileElf("0");
		assertEquals("", exec("", path).out);
	}

	private Path compileElf(String program) {
		String program1 = "" //
				+ "asm {" //
				+ "	MOV (EBP, ESP);" //
				+ "} / ((" + program + ") | (i => asm {" //
				+ "	MOV (EBX, `EBP + 8`);" //
				+ "	MOV (EAX, 1);" //
				+ "	INT (-128);" //
				+ "}))" //
		;

		Path path = TempDir.resolve("a.out");
		elf.write(path, offset -> Funp_.main().compile(offset, program1));
		return path;
	}

	private Execute exec(String text, Path path) {
		Execute exec = new Execute(new String[] { path.toString(), }, text);
		assertEquals(0, exec.code);
		return exec;
	}

}
