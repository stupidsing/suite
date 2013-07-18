package org.suite;

import java.io.IOException;
import java.util.Arrays;

import org.suite.doer.ProverConfig;

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
