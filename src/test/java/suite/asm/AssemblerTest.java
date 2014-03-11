package suite.asm;

import org.junit.Test;

import suite.primitive.Bytes;

public class AssemblerTest {

	@Test
	public void test() {
		Bytes bytes = new Assembler().assemble(""//
				+ "	JMP .end \n" //
				+ "	MOV AX,16 \n" //
				+ "	MOV EAX,16 \n" //
				+ ".end \n" //
				+ "	ADVANCE 16 \n" //
		);
		System.out.println(bytes);
	}

}
