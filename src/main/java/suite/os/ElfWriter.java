package suite.os;

import java.io.IOException;
import java.io.OutputStream;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfWriter {

	private class Bb {
		private BytesBuilder bb = new BytesBuilder();

		public Bb db(int i) {
			return d(1, i);
		}

		public Bb dw(int i) {
			return d(2, i);
		}

		public Bb dd(int i) {
			return d(4, i);
		}

		public Bb append(byte bs[]) {
			bb.append(bs);
			return this;
		}

		private Bb d(int n, int i) {
			for (int j = 0; j < n; j++) {
				bb.append((byte) (i & 0xFF));
				i = i >> 8;
			}
			return this;
		}

		public Bytes toBytes() {
			return bb.toBytes();
		}
	}

	public void write(int org, Bytes code, OutputStream os) throws IOException {
		Bytes header = new Bb() //
				.db(0x7F) // e_ident
				.append("ELF".getBytes(FileUtil.charset)) //
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
				.dd(5) // p_flags
				.dd(0x1000) // p_align
				.toBytes();

		os.write(header.toBytes());
		os.write(code.toBytes());
	}

}
