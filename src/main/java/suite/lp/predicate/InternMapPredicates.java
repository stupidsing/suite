package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.TreeIntern;
import suite.node.util.IdentityKey;

public class InternMapPredicates {

	private static Map<IdentityKey, Node> internMap = new ConcurrentHashMap<>();

	public static SystemPredicate internMapClear = (prover, ps) -> {
		internMap.clear();
		TreeIntern.clear();
		return true;
	};

	public static SystemPredicate internMapContains = (prover, ps) -> {
		IdentityKey key = new IdentityKey(ps.finalNode());
		return internMap.containsKey(key);
	};

	public static SystemPredicate internMapPut = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		IdentityKey key = new IdentityKey(params[0].finalNode());
		return prover.bind(internMap.computeIfAbsent(key, any -> new Reference()), params[1]);
	};

}
