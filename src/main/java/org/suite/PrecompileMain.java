package org.suite;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.suite.doer.Prover;
import org.suite.node.Node;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	private static List<String> libraryNames = //
	Arrays.asList("STANDARD", "MATH");

	public static void main(String args[]) throws IOException {
		if (args.length > 0)
			libraryNames = Arrays.asList(args);

		for (String libraryName : libraryNames) {
			System.out.println("Pre-compiling " + libraryName + "... ");

			String imports[] = { "auto.sl", "fc-precompile.sl" };
			Prover prover = SuiteUtil.getProver(imports);
			Node node = SuiteUtil.parse("fc-setup-precompile " + libraryName);

			if (prover.prove(node))
				System.out.println("Pre-compilation success\n");
			else {
				System.out.println("Pre-compilation failed");
				return;
			}
		}

		System.out.println("please refresh eclipse workspace");
	}

}
