package suite.node.parser;

import suite.node.io.TermOp;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.To;

// mAIN=suite.node.parser.RecursiveFactorizerMain ./run.sh
public class RecursiveFactorizerMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(RecursiveFactorizerMain.class, args);
	}

	protected boolean run(String[] args) {
		var recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
		var s0 = To.string(System.in);
		String sx = recursiveFactorizer.rewrite(args[0], args[1], s0);
		System.out.println(sx);
		return true;
	}

}
