package suite.asm;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import suite.primitive.Bytes;
import suite.util.To;

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

	@Test
	public void testBootSector() throws IOException {
		Bytes bytes = new Assembler().assemble(To.string(new File("src/main/asm/bootloader.asm")));
		System.out.println(bytes);
	}

}
