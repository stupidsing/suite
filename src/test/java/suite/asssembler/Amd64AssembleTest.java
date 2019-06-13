package suite.asssembler;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64Assemble;

public class Amd64AssembleTest {

	private Amd64 amd64 = Amd64.me;

	@Test
	public void test() {
		var push = amd64.instruction(Insn.PUSH, amd64.imm(0l, 1));
		var bytes = new Amd64Assemble(8).assemble(0l, List.of(push), true);
		System.out.println(bytes);
		assertEquals(2, bytes.size());
	}

}