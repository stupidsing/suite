import org.junit.Test;

import suite.Suite;
import suite.asm.Assembler;

public class MoliuTest {

	@Test
	public void testAssemblePerformance() {
		new Assembler(32).assemble(Suite.parse(".org = 0, .l CLD (),"));
	}

}
