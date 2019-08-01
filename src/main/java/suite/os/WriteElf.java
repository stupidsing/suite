package suite.os;

import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;

import primal.Verbs.Get;
import suite.cfg.Defaults;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.serialize.SerOutput;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class WriteElf {

	private boolean isAmd64;
	private int org;
	private int fileHeaderSize;
	private int progHeaderSize;
	private int elfHeaderSize;

	public WriteElf(boolean isAmd64) {
		this.isAmd64 = isAmd64;
		org = isAmd64 ? 0x00400000 : 0x08048000;
		fileHeaderSize = isAmd64 ? 64 : 52;
		progHeaderSize = isAmd64 ? 56 : 32;
		elfHeaderSize = fileHeaderSize + progHeaderSize;
	}

	public Execute exec(byte[] input, Int_Obj<Bytes> source) {
		var path = Defaults.tmp("a.out." + Get.temp());

		write(org, source.apply(org + elfHeaderSize), path);
		return new Execute(new String[] { path.toString(), }, input);
	}

	private void write(int org, Bytes code, Path path) {
		FileUtil.out(path).doWrite(os -> {
			try (var do_ = SerOutput.of(os)) {
				write(org, code, do_);
			}
		});

		ex(() -> Files.setPosixFilePermissions(path, new HashSet<>(List.of( //
				PosixFilePermission.GROUP_EXECUTE, //
				PosixFilePermission.GROUP_READ, //
				PosixFilePermission.OTHERS_EXECUTE, //
				PosixFilePermission.OTHERS_READ, //
				PosixFilePermission.OWNER_EXECUTE, //
				PosixFilePermission.OWNER_READ, //
				PosixFilePermission.OWNER_WRITE))));
	}

	private void write(int org, Bytes code, SerOutput do_) throws IOException {
		do_.writeBytes(isAmd64 ? header64(org, code, do_) : header32(org, code, do_));
		do_.writeBytes(code);
	}

	private Bytes header32(int org, Bytes code, SerOutput do_) throws IOException {
		return new Write_() //
				.db(0x7F) // e_ident
				.append("ELF".getBytes(Defaults.charset)) //
				.append(new byte[] { 1, 1, 1, 0, }) //
				.append(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, }) //
				.dw(2) // e_type
				.dw(0x03) // e_machine
				.dd(1) // e_version
				.dd(org + elfHeaderSize) // e_entry
				.dd(fileHeaderSize) // e_phoff
				.dd(0) // e_shoff
				.dd(0) // e_flags
				.dw(fileHeaderSize) // e_ehsize
				.dw(progHeaderSize) // e_phentsize
				.dw(1) // e_phnum
				.dw(0) // e_shentsize
				.dw(0) // e_shnum
				.dw(0) // e_shstrndx
				.dd(1) // p_type
				.dd(0) // p_offset
				.dd(org) // p_vaddr
				.dd(org) // p_paddr
				.dd(code.size() + elfHeaderSize) // p_filesz
				.dd(code.size() + elfHeaderSize) // p_memsz
				.dd(7) // p_flags PF_R|PF_W|PF_X
				.dd(0x1000) // p_align
				.toBytes();
	}

	private Bytes header64(int org, Bytes code, SerOutput do_) throws IOException {
		return new Write_() //
				.db(0x7F) // e_ident
				.append("ELF".getBytes(Defaults.charset)) //
				.append(new byte[] { 2, 1, 1, 0, }) //
				.append(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, }) //
				.dw(2) // e_type
				.dw(0x3E) // e_machine
				.dd(1) // e_version
				.dq(org + elfHeaderSize) // e_entry
				.dq(fileHeaderSize) // e_phoff
				.dq(0) // e_shoff
				.dd(0) // e_flags
				.dw(fileHeaderSize) // e_ehsize
				.dw(progHeaderSize) // e_phentsize
				.dw(1) // e_phnum
				.dw(0) // e_shentsize
				.dw(0) // e_shnum
				.dw(0) // e_shstrndx
				.dd(1) // p_type
				.dd(7) // p_flags PF_R|PF_W|PF_X
				.dq(0) // p_offset
				.dq(org) // p_vaddr
				.dq(org) // p_paddr
				.dq(code.size() + elfHeaderSize) // p_filesz
				.dq(code.size() + elfHeaderSize) // p_memsz
				.dq(0x1000) // p_align
				.toBytes();
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

		private Write_ dq(long i) {
			return d(8, i);
		}

		private Write_ append(byte[] bs) {
			bb.append(bs);
			return this;
		}

		private Write_ d(int n, long i) {
			for (var j = 0; j < n; j++) {
				bb.append((byte) (i & 0xFF));
				i >>= 8;
			}
			return this;
		}

		private Bytes toBytes() {
			return bb.toBytes();
		}
	}

}
