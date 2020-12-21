package suite.node.parser;

import primal.Verbs.ReadString;
import suite.node.io.TermOp;
import suite.util.RunUtil;

// mAIN=suite.node.parser.RecursiveFactorizerMain ./run.sh
public class RecursiveFactorizerMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var recursiveFactorizer = new RecursiveFactorizer(TermOp.values);
			var s0 = ReadString.from(System.in);
			var sx = recursiveFactorizer.rewrite(args[0], args[1], s0);
			System.out.println(sx);
			return true;
		});
	}

}
