package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.doer.Prover;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.TreeIntern;
import suite.node.util.IdentityKey;

public class InternMapPredicates {

	private static Map<IdentityKey, Node> internMap = new ConcurrentHashMap<>();

	public static class InternMapClear implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			internMap.clear();
			TreeIntern.clear();
			return true;
		}
	}

	public static class InternMapPut implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			IdentityKey key = new IdentityKey(params[0].finalNode());
			return prover.bind(internMap.computeIfAbsent(key, any -> new Reference()), params[1]);
		}
	}

}
