package suite.node.parser;

import suite.node.io.TermOp;
import suite.util.RunUtil;
import suite.util.To;

// mAIN=suite.node.parser.RecursiveFactorizerMain ./run.sh
public class RecursiveFactorizerMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
			var s0 = To.string(System.in);
			var sx = recursiveFactorizer.rewrite(args[0], args[1], s0);
			System.out.println(sx);
			return true;
		});
	}

}
