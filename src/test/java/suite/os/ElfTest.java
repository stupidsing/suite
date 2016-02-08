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
				+ "asm _ MOV (EBP, ESP);" //
				+ "declare inc = function [i0, out ix,] ( {ix} = i0 + 1; );" //
				+ "declare int j;" //
				+ "inc [41, out j,];" //
				+ "j;" //
				+ "asm _ MOV (EBX, EAX);" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ INT (-128);";

		int org = 0x08048000;

		Bytes code = new ImperativeCompiler().compile(org + 84, program);

		try (OutputStream os = FileUtil.out(FileUtil.tmp + "/a.out")) {
			new ElfWriter().write(org, code, os);
		}
	}

}
