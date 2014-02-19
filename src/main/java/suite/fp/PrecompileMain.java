package suite.fp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import suite.Suite;
import suite.lp.doer.ProverConfig;
import suite.util.Util;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	private static final List<String> allLibraries = Arrays.asList( //
			"ARRAY", "CHARS", "FREQ", "HEAP", "MATCH", "MATH", "MONAD", "PERMUTE", "RB-TREE" //
	);

	public static void main(String args[]) throws IOException {
		final ProverConfig pc = new ProverConfig();

		Suite.precompile("STANDARD", pc);

		ThreadPoolExecutor executor = Util.createExecutorByProcessors();

		try {
			for (final String libraryName : allLibraries)
				executor.execute(new Runnable() {
					public void run() {
						Suite.precompile(libraryName, pc);
					}
				});
		} finally {
			executor.shutdown();
		}

		System.out.println("please refresh eclipse workspace");
	}
}
