package suite.node.parser;

import java.io.IOException;

import suite.node.io.TermOp;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// mvn assembly:single && java -cp target/suite-1.0-jar-with-dependencies.jar suite.node.parser.RecursiveFactorizerMain
public class RecursiveFactorizerMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(RecursiveFactorizerMain.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
		String s0 = To.string(System.in);
		String sx = recursiveFactorizer.rewrite(args[0], args[1], s0);
		System.out.println(sx);
		return true;
	}

}
