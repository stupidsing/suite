package suite.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import suite.ip.ImperativeCompiler;
import suite.primitive.Bytes;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class BootMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(BootMain.class, args);
	}

	@Override
	protected boolean run(String args[]) throws IOException {
		// Bytes kernel = new Assembler(32).assemble(To.string(new
		// File("src/main/asm/kernel.asm")));

		Bytes bootLoader = new Assembler(16).assemble(To.string(new File("src/main/asm/bootloader.asm")));
		Bytes kernel = new ImperativeCompiler().compile(0x40000, To.string(new File("src/main/resources/kernel.il")));

		assertEquals(512, bootLoader.size());
		assertTrue(kernel.size() < 65536);

		// Combine the images and align to 512 bytes
		Bytes disk0 = Bytes.concat(bootLoader, kernel);
		Bytes disk1 = disk0.pad(disk0.size() + 511 & 0xFFFFFE00, (byte) 0);

		Path path = Paths.get("target/boot.bin");
		Files.write(path, disk1.getBytes());

		System.out.println("cat target/boot.bin | dd bs=512 count=1 | /opt/udis86-1.7.2/udcli/udcli -16 | less");
		System.out.println("cat target/boot.bin | dd bs=512 skip=1 | /opt/udis86-1.7.2/udcli/udcli -32 | less");
		System.out.println("bochs -f src/main/asm/bochsrc");
		return true;
	}

}
