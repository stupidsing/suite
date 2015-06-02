package suite.lp.predicate;

import java.util.function.Predicate;

import suite.lp.doer.Prover;
import suite.node.Node;
import suite.node.Tree;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class PredicateUtil {

	public interface BuiltinPredicate {
		public boolean prove(Prover prover, Node ps);
	}

	public static BuiltinPredicate run(Sink<Node> fun) {
		return (prover, ps) -> {
			fun.sink(ps.finalNode());
			return true;
		};
	}

	public static BuiltinPredicate bool(Predicate<Node> fun) {
		return (prover, ps) -> fun.test(ps.finalNode());
	}

	public static BuiltinPredicate fun(Fun<Node, Node> fun) {
		return (prover, ps) -> {
			Node params[] = Tree.getParameters(ps, 2);
			Node p0 = params[0], p1 = params[1];
			return prover.bind(p1, fun.apply(p0.finalNode()));
		};
	}

	public static boolean tryProve(Prover prover, Predicate<Prover> pred) {
		Prover prover1 = new Prover(prover);
		boolean result = pred.test(prover1);
		if (!result) // Roll back bindings if overall goal is failed
			prover1.undoAllBinds();
		return result;
	}

}
