package suite.fp;

import java.io.IOException;
import java.util.Arrays;

import suite.Suite;
import suite.lp.doer.ProverConfig;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	public static void main(String args[]) throws IOException {
		for (String libraryName : Arrays.asList("STANDARD"))
			Suite.precompile(libraryName, new ProverConfig());

		System.out.println("please refresh eclipse workspace");
	}

}
