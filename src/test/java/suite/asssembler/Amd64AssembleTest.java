package suite.asssembler;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import primal.primitive.adt.Bytes;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Mode;

public class Amd64AssembleTest {

	private Amd64 amd64 = Amd64.me;

	@Test
	public void test() {
		var push = amd64.instruction(Insn.PUSH, amd64.imm(0l, 1));
		var bytes = new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(push), true);
		System.out.println(bytes);
		assertEquals(2, bytes.size());
	}

	@Test
	public void test16a() {
		var mov = amd64.instruction(Insn.MOV, amd64.mem(amd64.imm(0, 2), 2), amd64.imm(0x7042, 2));
		var bytes = new Amd64Assemble(Amd64Mode.REAL16).assemble(0l, List.of(mov), true);
		System.out.println(bytes);
		assertEquals(Bytes.of((byte) 0xC7, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x70), bytes);
	}

	@Test
	public void test16b() {
		var mov = amd64.instruction(Insn.MOV, amd64.mem(amd64.new OpImmLabel(2), 1), amd64.reg("DL"));
		var bytes = new Amd64Assemble(Amd64Mode.REAL16).assemble(0l, List.of(mov), true);
		System.out.println(bytes);
		assertEquals(Bytes.of((byte) 0xC7, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x70), bytes);
	}

	@Test
	public void testFail() {
		try {
			var mov = amd64.instruction(Insn.MOV, amd64.reg("BPL"), amd64.reg("AH"));
			new Amd64Assemble(Amd64Mode.LONG64).assemble(0l, List.of(mov), true);
			throw new RuntimeException();
		} catch (Exception e) {
		}
	}

}
