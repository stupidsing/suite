package suite.fp;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.os.LogUtil;
import suite.sample.Profiler;

public class PrecompileTest {

	@Test
	public void test() {
		System.out.println(new Profiler().profile(() -> {
			for (int i = 0; i < 3; i++)
				LogUtil.duration("", () -> Suite.precompile("STANDARD", new ProverConfig()));
		}));
	}

}
