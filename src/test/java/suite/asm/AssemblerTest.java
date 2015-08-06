package suite.asm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.primitive.Bytes;

public class AssemblerTest {

	@Test
	public void testAssemble() {
		boolean isProverTrace0 = Suite.isProverTrace;
		Suite.isProverTrace = true;
		try {
			Assembler assembler = new Assembler(32);
			assembler.assemble(Suite.parse(".org = 0, .l CLD (),"));
		} finally {
			Suite.isProverTrace = isProverTrace0;
		}
	}

	@Test
	public void testAssemblePerformance() {
		Assembler assembler = new Assembler(32);
		for (int i = 0; i < 4096; i++)
			assembler.assemble(Suite.parse(".org = 0, .l CLD (),"));
	}

	@Test
	public void testAssembler() {
		Bytes bytes = new Assembler(32).assemble("" //
				+ ".org = 0 \n" //
				+ "	JMP (.end) \n" //
				+ "	MOV (AX, 16) \n" //
				+ "	MOV (EAX, 16) \n" //
				+ ".end	() \n" //
				+ "	ADVANCE (16) \n" //
		);
		assertEquals(16, bytes.size());
		System.out.println(bytes);
	}

}
