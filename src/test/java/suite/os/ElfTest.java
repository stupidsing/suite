package suite.os;

import static org.junit.Assert.assertEquals;

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
import suite.util.DataOutput_;
import suite.util.TempDir;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	@Test
	public void test() throws IOException {
		String program = "" //
				+ "declare inc = function [i0, out ix,] ( {ix} = i0 + 1; ); \n" //
				+ "signature j = int; \n" //
				+ "inc [41, out j,]; \n" //
				+ "j; \n" //
		;

		compileElf(program);
	}

	@Test
	public void testCat() throws IOException {
		String program = "" //
				+ "declare linux-read = function [pointer:(byte * 256) buffer, int length,] ( \n" //
				+ "    buffer; \n" //
				+ "    asm _ MOV (ECX, EAX); \n" //
				+ "    length; \n" //
				+ "    asm _ MOV (EDX, EAX); \n" //
				+ "    asm _ MOV (EAX, 3); \n" //
				+ "    asm _ XOR (EBX, EBX); \n" //
				+ "    asm _ INT (-128); \n" //
				+ "    -- length in EAX \n" //
				+ "); \n" //
				+ "\n" //
				+ "\n" //
				+ "declare linux-write = function [pointer:(byte * 256) buffer, int length,] ( \n" //
				+ "    buffer; \n" //
				+ "    asm _ MOV (ECX, EAX); \n" //
				+ "    length; \n" //
				+ "    asm _ MOV (EDX, EAX); \n" //
				+ "    asm _ MOV (EAX, 4); \n" //
				+ "    asm _ XOR (EBX, EBX); \n" //
				+ "    asm _ INT (-128); \n" //
				+ "    -- length in EAX \n" //
				+ "); \n" //
				+ "\n" //
				+ "signature buffer = byte * 256; \n" //
				+ "declare nBytesRead = 1; \n" //
				+ "\n" //
				+ "while (nBytesRead != 0) do ( \n" //
				+ "    {nBytesRead} = linux-read [& buffer, 256,]; \n" //
				+ "    linux-write [& buffer, nBytesRead,]; \n" //
				+ "); \n" //
				+ "0; \n" //
		;

		String text = "garbage";
		Path path = compileElf(program);
		Execute exec = new Execute(new String[] { path.toString(), }, text);

		assertEquals(0, exec.code);
		assertEquals(text, exec.out);
	}

	private Path compileElf(String program) throws IOException {
		String program1 = "" //
				+ "asm _ MOV (EBP, ESP);" //
				+ program //
				+ "asm _ MOV (EBX, EAX);" //
				+ "asm _ MOV (EAX, 1);" //
				+ "asm _ INT (-128);";

		int org = 0x08048000;

		Bytes code = new ImperativeCompiler().compile(org + 84, program1);
		Path path = TempDir.resolve("a.out");

		try (OutputStream os = FileUtil.out(path); DataOutput_ do_ = DataOutput_.of(os)) {
			new ElfWriter().write(org, code, do_);
		}

		try {
			Files.setPosixFilePermissions(path, new HashSet<>(Arrays.asList( //
					PosixFilePermission.GROUP_EXECUTE, //
					PosixFilePermission.OTHERS_EXECUTE, //
					PosixFilePermission.OWNER_EXECUTE)));
		} catch (UnsupportedOperationException ex) {
		}

		return path;
	}

}
