package suite.fp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.os.LogUtil;
import suite.sample.Profiler;

public class PrecompileTest {

	private List<String> allLibraries = List.of( //
			"23-TREE", //
			"ARRAY", //
			"CHARS", //
			"DEBUG", //
			"FREQ", //
			"HEAP", //
			"MAPACCUM", //
			"MATCH", //
			"MATH", //
			"MONAD", //
			"PERMUTE", //
			"RB-TREE", //
			"SUITE", //
			"TEXT");

	@Test
	public void testPrecompileAll() {
		Read.from(allLibraries).sink(lib -> assertTrue(Suite.precompile(lib, new ProverCfg())));
	}

	@Test // long test
	public void testThreeTimes() {
		new Profiler().profile(() -> {
			for (var i = 0; i < 3; i++)
				LogUtil.duration("", () -> {
					var b = Suite.precompile("STANDARD", new ProverCfg());
					assertTrue(b);
					return b;
				});
		});
	}

}
