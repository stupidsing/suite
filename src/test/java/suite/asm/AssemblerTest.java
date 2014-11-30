package suite.asm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.Bytes;

public class AssemblerTest {

	@Test
	public void test() {
		Bytes bytes = new Assembler(32).assemble(".org = 0\n" //
				+ "	JMP (.end) \n" //
				+ "	MOV (AX, 16) \n" //
				+ "	MOV (EAX, 16) \n" //
				+ ".end () \n" //
				+ "	ADVANCE (16) \n" //
		);
		assertEquals(16, bytes.size());
		System.out.println(bytes);
	}

}
