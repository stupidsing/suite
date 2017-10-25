package suite.os;

import java.nio.file.Path;

import org.junit.Test;

import suite.funp.Funp_;
import suite.primitive.Bytes;
import suite.util.TempDir;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	@Test
	public void test() {
		compileElf("0");
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

		int org = 0x08048000;
		Bytes code = Funp_.main().compile(program1);
		Path path = TempDir.resolve("a.out");
		new ElfWriter().write(org, code, path);
		return path;
	}

}
