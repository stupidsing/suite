package suite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingBinder.BindPredicate;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingBinderImpl;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.VariableMapperImpl.Generalization;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.util.FunUtil.Fun;

public class BindArrayUtil {

	private Map<String, Fun<Node, Node[]>> matchers = new ConcurrentHashMap<>();

	// --------------------------------
	// bind utilities

	public Fun<Node, Node[]> matcher(String pattern) {
		return matchers.computeIfAbsent(pattern, pattern_ -> {
			Generalizer generalizer = new Generalizer();
			Node toMatch = generalizer.generalize(Suite.parse(pattern_));

			SewingBinderImpl sb = new SewingBinderImpl(false);
			BindPredicate pred = sb.compileBind(toMatch);
			List<Integer> indexList = new ArrayList<>();
			Integer index;
			int n = 0;
			while ((index = sb.getVariableIndex(generalizer.getVariable(Atom.of("." + n++)))) != null)
				indexList.add(index);

			int size = indexList.size();
			int indices[] = new int[size];
			for (int i = 0; i < size; i++)
				indices[i] = indexList.get(i);

			return node -> {
				Env env = sb.env();
				Trail trail = new Trail();
				BindEnv be = new BindEnv() {
					public Env getEnv() {
						return env;
					}

					public Trail getTrail() {
						return trail;
					}
				};
				if (pred.test(be, node)) {
					Node results[] = new Node[size];
					for (int i = 0; i < size; i++)
						results[i] = env.get(indices[i]);
					return results;
				} else
					return null;
			};
		});
	}

	public Node substitute(String pattern, Node... nodes) {
		Generalization generalization = SewingGeneralizerImpl.process(Suite.parse(pattern));
		int i = 0;

		for (Node node : nodes) {
			Node variable = generalization.getVariable(Atom.of("." + i++));
			((Reference) variable).bound(node);
		}

		return generalization.node;
	}

}
