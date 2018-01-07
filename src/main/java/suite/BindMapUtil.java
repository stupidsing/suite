package suite;

import java.util.Map;
import java.util.Map.Entry;

import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.BinderFactory.Bind_;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper.NodeEnv;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;

public class BindMapUtil {

	public interface Match {
		public Map<String, Node> apply(Node node);

		public Node substitute(Map<String, Node> map);
	}

	public Match match(String pattern) {
		return matches.apply(pattern);
	}

	private Fun<String, Match> matches = Memoize.fun(pattern_ -> {
		Generalizer generalizer = new Generalizer();
		Node node = Suite.parse(pattern_);

		CompileBinderImpl cb = new CompileBinderImpl(false);
		Bind_ pred = cb.binder(generalizer.generalize(node));

		SewingGeneralizerImpl sg = new SewingGeneralizerImpl();
		Source<NodeEnv<Atom>> source = sg.g(Suite.parse(pattern_));

		Map<String, Integer> map = Read //
				.from(generalizer.getVariableNames()) //
				.toMap(Formatter::display, name -> cb.vm.getIndex(generalizer.getVariable(name)));

		return new Match() {
			public Map<String, Node> apply(Node node) {
				Env env = cb.env();
				return pred.test(new BindEnv(env), node) ? Read.from2(map).mapValue(env::get).toMap() : null;
			}

			public Node substitute(Map<String, Node> map_) {
				NodeEnv<Atom> ne = source.source();
				Reference[] refs = ne.env.refs;
				for (Entry<String, Node> e : map_.entrySet())
					refs[map.get(e.getKey())].bound(e.getValue());
				return ne.node;
			}
		};

	});

}
