package suite.lp.predicate;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TermHashKey;
import suite.util.FunUtil.Source;

public class FindPredicates {

	private static Map<TermHashKey, Node> memoizedPredicates = new HashMap<>();

	public static class FindAll implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 3);
			return prover.bind(params[2], findAll(prover, params[0], params[1]));
		}
	}

	// memoize is not re-entrant due to using computeIfAbsent()
	public static class Memoize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 3);
			Reference var = (Reference) params[0];

			Node goal = params[1];
			TermHashKey key = new TermHashKey(new Cloner().clone(Tree.of(TermOp.SEP___, var, goal)));
			return prover.bind(params[2], memoizedPredicates.computeIfAbsent(key, k -> findAll(prover, var, goal)));
		}
	}

	public static class MemoizeClear implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			memoizedPredicates.clear();
			return true;
		}
	}

	private static Node findAll(Prover prover, Node var, Node goal) {
		Stack<Node> stack = new Stack<>();

		Tree subGoal = Tree.of(TermOp.AND___, goal, new Data<Source<Boolean>>(() -> {
			stack.push(new Cloner().clone(var));
			return Boolean.FALSE;
		}));

		new Prover(prover).elaborate(subGoal);

		Node result = Atom.NIL;
		while (!stack.isEmpty())
			result = Tree.of(TermOp.AND___, stack.pop(), result);
		return result;
	}

}
