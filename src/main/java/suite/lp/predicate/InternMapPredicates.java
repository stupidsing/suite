package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;
import suite.node.util.IdentityKey;

public class InternMapPredicates {

	private static Map<IdentityKey, Node> internMap = new ConcurrentHashMap<>();

	public SystemPredicate internMapClear = PredicateUtil.predicate(n -> {
		internMap.clear();
		TreeIntern.clear();
	});

	public SystemPredicate internMapContains = (prover, ps) -> {
		IdentityKey key = new IdentityKey(ps.finalNode());
		return internMap.containsKey(key);
	};

	public SystemPredicate internMapPut = PredicateUtil.funPredicate(n -> //
			internMap.computeIfAbsent(new IdentityKey(n), any -> new Reference()));

}
