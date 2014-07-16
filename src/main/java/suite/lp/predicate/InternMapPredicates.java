package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;
import suite.node.util.IdentityKey;

public class InternMapPredicates {

	private static Map<IdentityKey, Node> internMap = new ConcurrentHashMap<>();

	public static SystemPredicate internMapClear = SystemPredicates.predicate(n -> {
		internMap.clear();
		TreeIntern.clear();
	});

	public static SystemPredicate internMapContains = (prover, ps) -> {
		IdentityKey key = new IdentityKey(ps.finalNode());
		return internMap.containsKey(key);
	};

	public static SystemPredicate internMapPut = SystemPredicates.funPredicate(n -> //
			internMap.computeIfAbsent(new IdentityKey(n), any -> new Reference()));

}
