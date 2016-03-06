package suite.fp;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.os.LogUtil;
import suite.sample.Profiler;
import suite.streamlet.Read;

public class PrecompileTest {

	private List<String> allLibraries = Arrays.asList( //
			"23-TREE" //
			, "ARRAY" //
			, "CHARS" //
			, "DEBUG" //
			, "FREQ" //
			, "HEAP" //
			, "MAPACCUM" //
			, "MATCH" //
			, "MATH" //
			, "MONAD" //
			, "PERMUTE" //
			, "RB-TREE" //
			, "SUITE" //
			, "TEXT" //
	);

	@Test
	public void testPrecompileAll() {
		Read.from(allLibraries).sink(lib -> assertTrue(Suite.precompile(lib, new ProverConfig())));
	}

	@Test
	public void testThreeTimes() {
		new Profiler().profile(() -> {
			for (int i = 0; i < 3; i++)
				LogUtil.duration("", () -> {
					boolean b = Suite.precompile("STANDARD", new ProverConfig());
					assertTrue(b);
					return b;
				});
		});
	}

}
