package suite.asm;

import suite.lp.predicate.EvalPredicates;
import suite.lp.predicate.PredicateUtil;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;

public class AssemblePredicates {

	public static boolean isPass2;

	public BuiltinPredicate emit8 = emitBytes(1);
	public BuiltinPredicate emit16 = emitBytes(2);
	public BuiltinPredicate emit32 = emitBytes(4);

	private BuiltinPredicate emitBytes(int n) {
		return PredicateUtil.p3((prover, n0, p0, px) -> {
			int i = isPass2 ? new EvalPredicates().evaluate(n0) : 0;
			byte[] bytes = new byte[n];
			Node p = px;

			for (int j = 0; j < n; j++) {
				bytes[j] = (byte) (i & 0xFF);
				i >>= 8;
			}

			for (int j = n - 1; 0 <= j; j--)
				p = Tree.of(TermOp.AND___, Int.of(Byte.toUnsignedInt(bytes[j])), p);

			return prover.bind(p0, p);
		});
	}

}
