package org.suite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.suite.doer.Prover;
import org.suite.node.Node;
import org.util.IoUtil;

/**
 * Performs precompilation.
 * 
 * @author ywsing
 */
public class PrecompileMain {

	public static void main(String args[]) throws IOException {

		// Clears previous precompilation result
		File precompiled = new File("precompiled.rpn");
		precompiled.delete();
		IoUtil.writeStream(new FileOutputStream(precompiled), "\\\n");

		// Compiles again
		String imports[] = { "auto.sl", "fc-precompile.sl" };
		Prover prover = SuiteUtil.getProver(imports);
		Node node = SuiteUtil.parse("fc-setup-standard-precompile");

		if (prover.prove(node)) {
			System.out.println("Pre-compilation success");
			System.out.println("please refresh eclipse workspace");
		} else
			System.err.println("Pre-compilation failed");
	}

}
