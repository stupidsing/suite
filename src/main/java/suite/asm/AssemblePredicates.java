package suite.asm;

import suite.lp.doer.Prover;
import suite.lp.predicate.EvalPredicates;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

public class AssemblePredicates {

	public static boolean isPass2;

	public BuiltinPredicate emit8 = (prover, ps) -> emitBytes(prover, ps, 1);
	public BuiltinPredicate emit16 = (prover, ps) -> emitBytes(prover, ps, 2);
	public BuiltinPredicate emit32 = (prover, ps) -> emitBytes(prover, ps, 4);

	private boolean emitBytes(Prover prover, Node ps, int n) {
		Node params[] = TreeUtil.getParameters(ps, 3);
		byte bytes[] = new byte[n];
		Node n0 = params[0];
		Node node = params[2];
		int i = isPass2 ? new EvalPredicates().evaluate(n0) : 0;

		for (int j = 0; j < n; j++) {
			bytes[j] = (byte) (i & 0xFF);
			i >>= 8;
		}

		for (int j = n - 1; j >= 0; j--)
			node = Tree.of(TermOp.AND___, Int.of(Byte.toUnsignedInt(bytes[j])), node);

		return prover.bind(params[1], node);
	}

}
