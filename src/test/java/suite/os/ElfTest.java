package suite.os;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import suite.ip.ImperativeCompiler;
import suite.primitive.Bytes;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	@Test
	public void test() throws IOException {
		String program = "" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ MOV (EBX, 42);" //
				+ "asm _ INT (-128);";
		int org = 0x08048000;

		Bytes code = new ImperativeCompiler().compile(org, program);

		try (OutputStream os = FileUtil.out(FileUtil.tmp + "/a.out")) {
			new ElfWriter().write(org, code, os);
		}
	}

}
