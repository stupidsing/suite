package suite.fp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Performs precompilation.
 *
 * @author ywsing
 */
public class PrecompileMain extends ExecutableProgram {

	private List<String> allLibraries = Arrays.asList( //
			"23-TREE", "ARRAY", "CHARS", "FREQ", "HEAP", "MATCH", "MATH", "MONAD", "PERMUTE", "RB-TREE" //
	);

	public static void main(String args[]) throws IOException {
		Util.run(PrecompileMain.class, args);
	}

	protected boolean run(String args[]) {
		return precompile();
	}

	public boolean precompile() {
		ProverConfig pc = new ProverConfig();
		boolean ok = Suite.precompile("STANDARD", pc);
		List<Future<Boolean>> futures = new ArrayList<>();

		if (ok) {
			ThreadPoolExecutor executor = Util.createExecutorByProcessors();

			try {
				for (String libraryName : allLibraries)
					futures.add(executor.submit(() -> Suite.precompile(libraryName, pc)));
			} finally {
				executor.shutdown();
			}

			for (Future<Boolean> future : futures)
				try {
					ok &= future.get();
				} catch (InterruptedException | ExecutionException ex) {
					ex.printStackTrace();
					ok = false;
				}
		}

		if (ok)
			System.out.println("Please refresh eclipse workspace");
		else
			System.err.println("COMPILATION FAILURE");

		return ok;
	}

}
