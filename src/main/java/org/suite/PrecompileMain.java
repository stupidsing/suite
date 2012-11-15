package org.suite;

import java.io.IOException;
import java.util.Arrays;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	public static void main(String args[]) throws IOException {
		for (String libraryName : Arrays.asList("STANDARD", "MATH"))
			new Main().runPrecompile(libraryName);

		System.out.println("please refresh eclipse workspace");
	}

}
