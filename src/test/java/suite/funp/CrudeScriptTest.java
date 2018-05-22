package suite.funp;

import org.junit.Test;

import suite.inspect.Dump;

public class CrudeScriptTest {

	@Test
	public void test() {
		Dump.out(new P0CrudeScript().parse("{ return 1 + 2 * 3; }"));
	}

}
