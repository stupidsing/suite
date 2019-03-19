package suite.asm;

import static suite.util.Friends.fail;

import java.nio.file.Paths;

import suite.ip.ImperativeCompiler;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.RunUtil;
import suite.util.To;

// mvn compile exec:java -Dexec.mainClass=suite.asm.BootMain && qemu-system-x86_64 target/boot.bin
public class BootMain {

	public static void main(String[] args) {
		RunUtil.run(BootMain::main);
	}

	public static boolean main() {
		var bootLoader = new Assembler(16).assemble(FileUtil.read("src/main/asm/bootloader.asm"));
		var kernel = new ImperativeCompiler().compile(0x40000, Paths.get("src/main/il/kernel.il"));

		if (bootLoader.size() == 512 && kernel.size() < 65536) {

			// combine the images and align to 512 bytes
			var disk0 = Bytes.concat(bootLoader, kernel);
			var disk1 = disk0.pad(disk0.size() + 511 & 0xFFFFFE00);

			var image = "target/boot.bin";
			Read.each(disk1).collect(To.file(image));

			System.out.println("cat " + image + " | dd bs=512 count=1 | ~/udis86/udcli/udcli -16 | less");
			System.out.println("cat " + image + " | dd bs=512 skip=1 | ~/udis86/udcli/udcli -32 | less");
			System.out.println("qemu-system-x86_64 target/boot.bin");
			return true;
		} else
			return fail("size not match");
	}

}
