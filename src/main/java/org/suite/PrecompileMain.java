package org.suite;

import java.io.IOException;
import java.util.Arrays;

import org.suite.doer.Prover;
import org.suite.node.Node;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	public static void main(String args[]) throws IOException {
		for (String libraryName : Arrays.asList("STANDARD", "MATH"))
			precompileLibrary(libraryName);

		System.out.println("please refresh eclipse workspace");
	}

	public static void precompileLibrary(String libraryName) {
		System.out.println("Pre-compiling " + libraryName + "... ");

		String imports[] = { "auto.sl", "fc-precompile.sl" };
		Prover prover = SuiteUtil.getProver(imports);
		Node node = SuiteUtil.parse("fc-setup-precompile " + libraryName);

		if (prover.prove(node))
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed");
	}

}
