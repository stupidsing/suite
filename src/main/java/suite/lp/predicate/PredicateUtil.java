package suite.lp.predicate;

import java.util.function.Predicate;

import suite.lp.doer.Prover;
import suite.node.Node;
import suite.node.Tree;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class PredicateUtil {

	public interface SystemPredicate {
		public boolean prove(Prover prover, Node ps);
	}

	public static SystemPredicate predicate(Sink<Node> fun) {
		return (prover, ps) -> {
			fun.sink(ps.finalNode());
			return true;
		};
	}

	public static SystemPredicate boolPredicate(Predicate<Node> fun) {
		return (prover, ps) -> fun.test(ps.finalNode());
	}

	public static SystemPredicate funPredicate(Fun<Node, Node> fun) {
		return (prover, ps) -> {
			Node params[] = Tree.getParameters(ps, 2);
			Node p0 = params[0], p1 = params[1];
			return prover.bind(p1, fun.apply(p0.finalNode()));
		};
	}

}
