package suite.asm;

import suite.lp.predicate.PredicateUtil;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.tree.TreeAnd;
import suite.node.util.TreeUtil;

public class AssemblePredicates {

	public static boolean isPass2;

	public BuiltinPredicate emit8 = emitBytes(1);
	public BuiltinPredicate emit16 = emitBytes(2);
	public BuiltinPredicate emit32 = emitBytes(4);

	private BuiltinPredicate emitBytes(int n) {
		return PredicateUtil.p3((prover, n0, p0, px) -> {
			var i = isPass2 ? TreeUtil.evaluate(n0) : 0;
			var bytes = new byte[n];
			var p = px;

			for (var j = 0; j < n; j++) {
				bytes[j] = (byte) (i & 0xFF);
				i >>= 8;
			}

			for (var j = n - 1; 0 <= j; j--)
				p = TreeAnd.of(Int.of(Byte.toUnsignedInt(bytes[j])), p);

			return prover.bind(p0, p);
		});
	}

}
