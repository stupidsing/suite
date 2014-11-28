package suite.asm;

import suite.lp.doer.Prover;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;

public class AssemblePredicates {

	private Int zero = Int.of(0);

	public BuiltinPredicate emit8 = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		Node n0 = params[0].finalNode();
		Node i0;

		if (n0 instanceof Int)
			i0 = n0;
		else if (n0 instanceof Reference)
			i0 = zero;
		else
			return false;

		return prover.bind(params[1], Tree.of(TermOp.AND___, i0, params[2]));
	};

	public BuiltinPredicate emit16 = (prover, ps) -> emitBytes(prover, ps, 2);

	public BuiltinPredicate emit32 = (prover, ps) -> emitBytes(prover, ps, 4);

	private boolean emitBytes(Prover prover, Node ps, int n) {
		Node params[] = Tree.getParameters(ps, 3);
		Node n0 = params[0].finalNode();
		int i;

		if (n0 instanceof Int)
			i = ((Int) n0).number;
		else if (n0 instanceof Reference)
			i = 0;
		else
			return false;

		byte bytes[] = new byte[n];
		for (int j = 0; j < n; j++) {
			bytes[j] = (byte) (i & 0xFF);
			i >>= 8;
		}

		Node node = params[2];
		for (int j = n - 1; j >= 0; j--)
			node = Tree.of(TermOp.AND___, Int.of(Byte.toUnsignedInt(bytes[j])), node);

		return prover.bind(params[1], node);
	}

}
