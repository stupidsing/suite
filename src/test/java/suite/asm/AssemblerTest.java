package suite.asm;

import org.junit.Test;

import suite.primitive.Bytes;

public class AssemblerTest {

	@Test
	public void test() {
		Bytes bytes = new Assembler().assemble(""//
				+ "	JMP dword .end \n" //
				+ "	MOV EAX,16 \n" //
				+ ".end \n" //
		);
		System.out.println(bytes);
	}

}
