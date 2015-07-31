package suite.lp.predicate;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TermKey;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class FindPredicates {

	private static Map<TermKey, Node> memoizedPredicates = new ConcurrentHashMap<>();

	public BuiltinPredicate findAll = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		Node var = params[0], goal = params[1], results = params[2];
		return prover.bind(results, findAll(prover, var, goal));
	};

	// memoize is not re-entrant due to using computeIfAbsent()
	public BuiltinPredicate findAllMemoized = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		Node var = params[0], goal = params[1], results = params[2];
		TermKey key = new TermKey(new Cloner().clone(Tree.of(TermOp.SEP___, var, goal)));
		return prover.bind(results, memoizedPredicates.computeIfAbsent(key, k -> findAll(prover, var, goal)));
	};

	public BuiltinPredicate findAllMemoizedClear = PredicateUtil.run(n -> memoizedPredicates.clear());

	public BuiltinPredicate suspend = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		Node p0 = params[0];
		Node p1 = params[1];
		Node p2 = params[2];
		Suspend suspend = new Suspend(() -> Read.from(elaborate(prover, p1, p2)).uniqueResult());

		if (p0 instanceof Reference) {
			prover.getJournal().addBind((Reference) p0, suspend);
			return true;
		} else
			return false;
	};

	private Node findAll(Prover prover, Node var, Node goal) {
		Stack<Node> stack = elaborate(prover, var, goal);
		Node result = Atom.NIL;
		while (!stack.isEmpty())
			result = Tree.of(TermOp.AND___, stack.pop(), result);
		return result;
	}

	private Stack<Node> elaborate(Prover prover, Node var, Node goal) {
		Stack<Node> stack = new Stack<>();

		Tree subGoal = Tree.of(TermOp.AND___, goal, new Data<Source<Boolean>>(() -> {
			stack.push(new Cloner().clone(var));
			return Boolean.FALSE;
		}));

		new Prover(prover).elaborate(subGoal);
		return stack;
	}

}
