package suite.lp.predicate;

import java.util.function.Predicate;

import suite.lp.doer.Prover;
import suite.node.Node;
import suite.node.util.TreeUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class PredicateUtil {

	public interface BuiltinPredicate {
		public boolean prove(Prover prover, Node ps);
	}

	public static BuiltinPredicate run(Sink<Node> fun) {
		return (prover, ps) -> {
			fun.sink(ps);
			return true;
		};
	}

	public static BuiltinPredicate bool(Predicate<Node> fun) {
		return (prover, ps) -> fun.test(ps);
	}

	public static BuiltinPredicate fun(Fun<Node, Node> fun) {
		return (prover, ps) -> {
			Node params[] = TreeUtil.getElements(ps, 2);
			Node p0 = params[0], p1 = params[1];
			return prover.bind(p1, fun.apply(p0));
		};
	}

	public static boolean tryProve(Prover prover, Fun<Prover, Boolean> source) {
		Prover prover1 = new Prover(prover);
		boolean result = false;
		try {
			result = source.apply(prover1);
		} finally {
			if (!result) // Roll back bindings if overall goal is failed
				prover1.undoAllBinds();
		}
		return result;
	}

}
