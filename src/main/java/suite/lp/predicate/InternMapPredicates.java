package suite.lp.predicate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
import suite.node.Node;
import suite.node.Tree;
import suite.node.util.IdHashKey;

public class InternMapPredicates {

	private static Map<IdHashKey, Node> internMap = new ConcurrentHashMap<>();

	public static class InternMapClear implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			internMap.clear();
			return true;
		}
	}

	public static class InternMapGet implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			IdHashKey key = new IdHashKey(params[0].finalNode());
			return internMap.containsKey(key) && prover.bind(params[1], internMap.get(key));
		}
	}

	public static class InternMapPut implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			IdHashKey key = new IdHashKey(params[0].finalNode());
			internMap.put(key, new Cloner().clone(params[1]));
			return true;
		}
	}

}
