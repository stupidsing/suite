package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.IdentityKey;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;

public class InternMapPredicates {

	private static Map<IdentityKey<Node>, Node> internMap = new ConcurrentHashMap<>();

	public BuiltinPredicate internMapClear = PredicateUtil.run(n -> {
		internMap.clear();
		TreeIntern.clear();
	});

	public BuiltinPredicate internMapContains = (prover, ps) -> {
		IdentityKey<Node> key = IdentityKey.of(ps.finalNode());
		return internMap.containsKey(key);
	};

	public BuiltinPredicate internMapPut = PredicateUtil.fun(n -> //
			internMap.computeIfAbsent(IdentityKey.of(n), any -> new Reference()));

}
