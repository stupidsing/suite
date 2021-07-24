import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.lp.Configuration;

public class MoliuTest {

	@Test
	public void test() {
		for (var i = 0; i < 3; i++)
			assertTrue(Suite.precompile("STANDARD_FAIL", new Configuration.ProverCfg()));

		assertTrue(Suite.precompile("FAIL", new Configuration.ProverCfg()));
	}

}
