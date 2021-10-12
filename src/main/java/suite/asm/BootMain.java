package suite.asm;

import static primal.statics.Fail.fail;

import java.nio.file.Paths;

import primal.MoreVerbs.Read;
import primal.Verbs.ReadString;
import primal.primitive.adt.Bytes;
import suite.assembler.Amd64Mode;
import suite.ip.ImperativeCompiler;
import suite.util.RunUtil;
import suite.util.To;

// mvn compile exec:java -Dexec.mainClass=suite.asm.BootMain && qemu-system-x86_64 target/boot.bin
public class BootMain {

	public static void main(String[] args) {
		RunUtil.run(BootMain::main);
	}

	public static boolean main() {
		var bootLoader = new Assembler(Amd64Mode.REAL16).assemble(ReadString.from("src/main/asm/bootloader.asm"));
		var kernel = new ImperativeCompiler().compile(0x40000, Paths.get("src/main/il/kernel.il"));

		if (bootLoader.size() == 512 && kernel.size() < 65536) {

			// combine the images and align to 512 bytes
			var disk0 = Bytes.concat(bootLoader, kernel);
			var disk1 = disk0.pad(disk0.size() + 511 & 0xFFFFFE00);
			var image = "target/boot.bin";

			Read.each(disk1).collect(To.file(image));

			System.out.println("cat " + image + " | dd bs=512 count=1 | tp_udcli -16 | less");
			System.out.println("cat " + image + " | dd bs=512 skip=1 | tp_udcli -32 | less");
			System.out.println("qemu-system-x86_64 " + image);
			return true;
		} else
			return fail("size not match");
	}

}
