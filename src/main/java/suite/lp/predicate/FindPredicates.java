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
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.util.Memoize;

public class FindPredicates {

	private static Map<TermKey, Source<Node>> memoizedPredicates = new ConcurrentHashMap<>();

	public BuiltinPredicate findAll = PredicateUtil
			.p3((prover, var, goal, results) -> prover.bind(results, findAll(prover, var, goal)));

	// memoize is not re-entrant due to using computeIfAbsent()
	public BuiltinPredicate findAllMemoized = PredicateUtil.p3((prover, var, goal, results) -> {
		var key = new TermKey(new Cloner().clone(Tree.of(TermOp.SEP___, var, goal)));
		Node results_ = memoizedPredicates.computeIfAbsent(key, k -> Memoize.future(() -> findAll(prover, var, goal))).g();
		return prover.bind(results, results_);
	});

	public BuiltinPredicate findAllMemoizedClear = PredicateUtil.run(memoizedPredicates::clear);

	public BuiltinPredicate suspend = PredicateUtil.p3((prover, susp, var, goal) -> {
		var suspend = new Suspend(() -> Read.from(elaborate(prover, var, goal)).uniqueResult());

		if (susp instanceof Reference) {
			prover.getTrail().addBind((Reference) susp, suspend);
			return true;
		} else
			return false;
	});

	private Node findAll(Prover prover, Node var, Node goal) {
		Stack<Node> stack = elaborate(prover, var, goal);
		Node result = Atom.NIL;
		while (!stack.isEmpty())
			result = Tree.ofAnd(stack.pop(), result);
		return result;
	}

	private Stack<Node> elaborate(Prover prover, Node var, Node goal) {
		Stack<Node> stack = new Stack<>();

		Tree subGoal = Tree.ofAnd(goal, new Data<Source<Boolean>>(() -> {
			stack.push(new Cloner().clone(var));
			return Boolean.FALSE;
		}));

		new Prover(prover).elaborate(subGoal);
		return stack;
	}

}
