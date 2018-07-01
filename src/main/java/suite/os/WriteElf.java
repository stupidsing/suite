package suite.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;

import suite.Defaults;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.util.Fail;
import suite.util.SerOutput;
import suite.util.Util;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class WriteElf {

	public Execute exec(byte[] input, Int_Obj<Bytes> source) {
		var path = Defaults.tmp("a.out." + Util.temp());
		var org = 0x08048000;

		write(org, source.apply(org + 84), path);
		return new Execute(new String[] { path.toString(), }, input);
	}

	private void write(int org, Bytes code, Path path) {
		FileUtil.out(path).write(os -> {
			try (var do_ = SerOutput.of(os)) {
				write(org, code, do_);
			}
		});

		try {
			Files.setPosixFilePermissions(path, new HashSet<>(List.of( //
					PosixFilePermission.GROUP_EXECUTE, //
					PosixFilePermission.GROUP_READ, //
					PosixFilePermission.OTHERS_EXECUTE, //
					PosixFilePermission.OTHERS_READ, //
					PosixFilePermission.OWNER_EXECUTE, //
					PosixFilePermission.OWNER_READ, //
					PosixFilePermission.OWNER_WRITE)));
		} catch (UnsupportedOperationException ex) {
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void write(int org, Bytes code, SerOutput do_) throws IOException {
		var header = new Write_() //
				.db(0x7F) // e_ident
				.append("ELF".getBytes(Defaults.charset)) //
				.append(new byte[] { 1, 1, 1, 0, }) //
				.append(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, }) //
				.dw(2) // e_type
				.dw(3) // e_machine
				.dd(1) // e_version
				.dd(org + 84) // e_entry
				.dd(52) // e_phoff
				.dd(0) // e_shoff
				.dd(0) // e_flags
				.dw(52) // e_ehsize
				.dw(32) // e_phentsize
				.dw(1) // e_phnum
				.dw(0) // e_shentsize
				.dw(0) // e_shnum
				.dw(0) // e_shstrndx
				.dd(1) // p_type
				.dd(0) // p_offset
				.dd(org) // p_vaddr
				.dd(org) // p_paddr
				.dd(code.size() + 84) // p_filesz
				.dd(code.size() + 84) // p_memsz
				.dd(7) // p_flags PF_R|PF_W|PF_X
				.dd(0x1000) // p_align
				.toBytes();

		do_.writeBytes(header);
		do_.writeBytes(code);
	}

	private class Write_ {
		private BytesBuilder bb = new BytesBuilder();

		private Write_ db(int i) {
			return d(1, i);
		}

		private Write_ dw(int i) {
			return d(2, i);
		}

		private Write_ dd(int i) {
			return d(4, i);
		}

		private Write_ append(byte[] bs) {
			bb.append(bs);
			return this;
		}

		private Write_ d(int n, int i) {
			for (var j = 0; j < n; j++) {
				bb.append((byte) (i & 0xFF));
				i = i >> 8;
			}
			return this;
		}

		private Bytes toBytes() {
			return bb.toBytes();
		}
	}

}
