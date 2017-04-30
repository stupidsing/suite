package suite;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingBinder.BindPredicate;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingBinderImpl0;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.VariableMapperImpl.Generalization;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.Memoize;

public class BindMapUtil {

	private Fun<String, Fun<Node, Map<String, Node>>> matchers = Memoize.fun(pattern_ -> {
		Generalizer generalizer = new Generalizer();
		Node toMatch = generalizer.generalize(Suite.parse(pattern_));

		SewingBinderImpl0 sb = new SewingBinderImpl0(false);
		BindPredicate pred = sb.compileBind(toMatch);
		Streamlet2<String, Integer> indices = Read.from(generalizer.getVariablesNames()) //
				.map2(Formatter::display, name -> sb.getVariableIndex(generalizer.getVariable(name))) //
				.collect(As::streamlet2);

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
				Map<String, Node> results = new HashMap<>();
				indices.sink((name, index) -> results.put(name, env.get(index)));
				return results;
			} else
				return null;
		};
	});

	// --------------------------------
	// bind utilities

	public Fun<Node, Map<String, Node>> matcher(String pattern) {
		return matchers.apply(pattern);
	}

	public Node substitute(String pattern, Map<String, Node> map) {
		Generalization generalization = SewingGeneralizerImpl.process(Suite.parse(pattern));

		for (Entry<String, Node> e : map.entrySet()) {
			Node variable = Atom.of(e.getKey());
			((Reference) variable).bound(e.getValue());
		}

		return generalization.node;
	}

}
