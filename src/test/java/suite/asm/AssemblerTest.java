package suite.asm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import suite.primitive.Bytes;
import suite.util.To;

public class AssemblerTest {

	@Test
	public void test() {
		Bytes bytes = new Assembler(32).assemble(""//
				+ "	JMP .end \n" //
				+ "	MOV AX,16 \n" //
				+ "	MOV EAX,16 \n" //
				+ ".end \n" //
				+ "	ADVANCE 16 \n" //
		);
		System.out.println(bytes);
	}

	// cat target/boot.bin | ~/data/src/udis86-1.7.2/udcli/udcli -16 |
	// less
	// bochs -f src/main/asm/bochsrc
	@Test
	public void testBootSector() throws IOException {
		Bytes bootLoader = new Assembler(16).assemble(To.string(new File("src/main/asm/bootloader.asm")));
		Bytes kernel = new Assembler(32).assemble(To.string(new File("src/main/asm/kernel.asm")));

		assertEquals(512, bootLoader.size());
		Bytes disk0 = Bytes.concat(bootLoader, kernel);

		// Align to 512 bytes
		int size = disk0.size();
		Bytes disk1 = disk0.pad((size + 511) & 0xFFFFFE00, (byte) 0);

		Path path = Paths.get("target/boot.bin");
		Files.write(path, disk1.getBytes());
	}

}
