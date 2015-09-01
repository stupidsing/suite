package suite.lp.predicate;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

public class InternPredicates {

	private static ThreadLocal<Map<IdentityKey<Node>, Node>> internMap = ThreadLocal.withInitial(() -> new HashMap<>());
	private static ThreadLocal<TreeIntern> treeIntern_ = ThreadLocal.withInitial(() -> new TreeIntern());

	public BuiltinPredicate internMapClear = PredicateUtil.run(n -> {
		internMap.get().clear();
		treeIntern_.get().clear();
	});

	public BuiltinPredicate internMapContains = (prover, ps) -> {
		IdentityKey<Node> key = IdentityKey.of(ps);
		return internMap.get().containsKey(key);
	};

	public BuiltinPredicate internMapPut = PredicateUtil.fun(n -> //
	internMap.get().computeIfAbsent(IdentityKey.of(n), any -> new Reference()));

	public BuiltinPredicate internTree = (prover, ps) -> {
		Node params[] = TreeUtil.getParameters(ps, 4);
		Node p = params[0];
		Node p1 = params[1];
		Node p2 = params[2];
		Node p3 = params[3];

		Operator operator = TermOp.find(((Atom) p2).name);
		return prover.bind(p, treeIntern_.get().of(operator, p1, p3));
	};

}
