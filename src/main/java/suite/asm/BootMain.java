package suite.asm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import suite.ip.ImperativeCompiler;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;
import suite.util.Util.RunOption;

// MAIN=suite.asm.BootMain ./run.sh
public class BootMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(BootMain.class, args, RunOption.TIME___);
	}

	@Override
	protected boolean run(String args[]) throws IOException {
		Bytes bootLoader = new Assembler(16).assemble(FileUtil.read("src/main/asm/bootloader.asm"));
		Bytes kernel = new ImperativeCompiler().compile(0x40000, Paths.get("src/main/il/kernel.il"));

		if (bootLoader.size() == 512 && kernel.size() < 65536) {

			// Combine the images and align to 512 bytes
			Bytes disk0 = Bytes.concat(bootLoader, kernel);
			Bytes disk1 = disk0.pad(disk0.size() + 511 & 0xFFFFFE00);

			String image = "target/boot.bin";
			Files.write(Paths.get(image), disk1.toBytes());

			System.out.println("cat " + image + " | dd bs=512 count=1 | /opt/udis86-1.7.2/udcli/udcli -16 | less");
			System.out.println("cat " + image + " | dd bs=512 skip=1 | /opt/udis86-1.7.2/udcli/udcli -32 | less");
			System.out.println("qemu-system-i386 target/boot.bin");
			if (Boolean.FALSE)
				System.out.println("" //
						+ "${BOCHS}/bochs \\\n" //
						+ "config_interface:textconfig \\\n" //
						+ "display_library:x \\\n" //
						+ "ata0-master:type=disk,path=\"target/boot.bin\",cylinders=615,heads=6,spt=17 \\\n" //
						+ "boot:disk \\\n" //
						+ "cpuid:sep=1 \\\n" //
						+ "romimage:file=${BOCHS}/bios/BIOS-bochs-latest,address=0xfffe0000 \\\n" //
						+ "vgaromimage:file=${BOCHS}/bios/VGABIOS-lgpl-latest\n" //
				);

			return true;
		} else
			throw new RuntimeException("Size not match");
	}

}
