package suite.os;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import suite.ip.ImperativeCompiler;
import suite.primitive.Bytes;
import suite.util.TempDir;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	@Test
	public void test() throws IOException {
		String program = "" //
				+ "declare inc = function [i0, out ix,] ( {ix} = i0 + 1; );" //
				+ "signature j = int;" //
				+ "inc [41, out j,];" //
				+ "j;";

		String program1 = "" //
				+ "asm _ MOV (EBP, ESP);" //
				+ program //
				+ "asm _ MOV (EBX, EAX);" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ INT (-128);";

		int org = 0x08048000;

		Bytes code = new ImperativeCompiler().compile(org + 84, program1);
		Path path = TempDir.resolve("a.out");

		try (OutputStream os = FileUtil.out(path)) {
			new ElfWriter().write(org, code, os);
		}

		try {
			Files.setPosixFilePermissions(path,
					new HashSet<>(Arrays.asList( //
							PosixFilePermission.GROUP_EXECUTE, //
							PosixFilePermission.OTHERS_EXECUTE, //
							PosixFilePermission.OWNER_EXECUTE)));
		} catch (UnsupportedOperationException ex) {
		}
	}

}
