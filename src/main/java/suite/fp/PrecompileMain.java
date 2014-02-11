package suite.fp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import suite.Suite;
import suite.lp.doer.ProverConfig;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	private static final List<String> allLibraries = Arrays.asList( //
			"FREQ", "MATH", "MONAD", "PERMUTE", "RB-TREE", "STANDARD" //
	);

	public static void main(String args[]) throws IOException {
		ProverConfig pc = new ProverConfig();

		for (String libraryName : allLibraries)
			Suite.precompile(libraryName, pc);

		System.out.println("please refresh eclipse workspace");
	}

}
