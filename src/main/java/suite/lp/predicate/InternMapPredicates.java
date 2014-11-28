package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;
import suite.node.util.IdentityKey;

public class InternMapPredicates {

	private static Map<IdentityKey, Node> internMap = new ConcurrentHashMap<>();

	public BuiltinPredicate internMapClear = PredicateUtil.run(n -> {
		internMap.clear();
		TreeIntern.clear();
	});

	public BuiltinPredicate internMapContains = (prover, ps) -> {
		IdentityKey key = new IdentityKey(ps.finalNode());
		return internMap.containsKey(key);
	};

	public BuiltinPredicate internMapPut = PredicateUtil.fun(n -> //
			internMap.computeIfAbsent(new IdentityKey(n), any -> new Reference()));

}
