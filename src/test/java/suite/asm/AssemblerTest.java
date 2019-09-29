package suite.asm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;

public class AssemblerTest {

	@Test
	public void testAssemble() {
		var isProverTrace0 = Suite.isProverTrace;
		Suite.isProverTrace = true;
		try {
			var assembler = new Assembler(32);
			assembler.assemble(Suite.parse(".org = 0, .l LEA (EAX, `EAX * 4 + (-4)`),"));
		} finally {
			Suite.isProverTrace = isProverTrace0;
		}
	}

	@Test
	public void testAssembleLongMode() throws IOException {
		var assembler = new Assembler(64, true);
		var bytes = assembler.assemble(Suite.parse(".org = 0, .l MOV (R9D, DWORD 16),"));
		assertEquals(7, bytes.size());
	}

	@Test
	public void testAssembleMisc() throws IOException {
		var assembler = new Assembler(32, true);
		assembler.assemble(Suite.parse(".org = 0, _ JMP (BYTE .label), .label (),"));
	}

	@Test
	public void testAssemblePerformance() {
		var assembler = new Assembler(32);
		for (var i = 0; i < 4096; i++)
			assembler.assemble(Suite.parse(".org = 0, .l CLD (),"));
	}

	@Test
	public void testAssembler() {
		var bytes = new Assembler(32).assemble("" //
				+ ".org = 0 \n" //
				+ "	JMP (.end) \n" //
				+ "	MOV (AX, WORD 16) \n" //
				+ "	MOV (EAX, 16) \n" //
				+ ".end	() \n" //
				+ "	ADVANCE (16) \n" //
		);
		assertEquals(16, bytes.size());
		System.out.println(bytes);
	}

}
