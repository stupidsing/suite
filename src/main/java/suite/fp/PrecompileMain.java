package suite.fp;

import java.io.IOException;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.os.LogUtil;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Performs precompilation.
 *
 * @author ywsing
 */
public class PrecompileMain extends ExecutableProgram {

	public static void main(String[] args) throws IOException {
		Util.run(PrecompileMain.class, args);
	}

	protected boolean run(String[] args) {
		return LogUtil.duration(getClass().getSimpleName(), () -> {
			ProverConfig pc = new ProverConfig();
			boolean ok = Suite.precompile("STANDARD", pc);

			if (ok)
				System.out.println("Please refresh eclipse workspace");
			else
				System.err.println("COMPILATION FAILURE");

			return ok;
		});
	}

}
