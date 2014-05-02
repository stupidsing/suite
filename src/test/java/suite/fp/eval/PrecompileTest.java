package suite.fp.eval;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.sample.Profiler;

public class PrecompileTest {

	@Test
	public void test() {
		System.out.println(new Profiler().profile(() -> {
			ProverConfig pc = new ProverConfig();
			Suite.precompile("STANDARD", pc);
		}));
	}

}
