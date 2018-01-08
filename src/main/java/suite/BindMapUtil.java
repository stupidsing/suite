package suite;

import java.util.Map;
import java.util.Map.Entry;

import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.BinderFactory.Bind_;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
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
		GeneralizerFactory sg = new SewingGeneralizerImpl();
		Source<NodeEnv<Atom>> sgs = sg.g(Suite.parse(pattern_));
		NodeEnv<Atom> ne = sgs.source();

		BinderFactory cb = new CompileBinderImpl(false);
		Bind_ pred = cb.binder(ne.node);

		VariableMapper<Atom> sgm = sg.mapper();
		VariableMapper<Reference> cbm = cb.mapper();
		Map<String, Integer> sgm_ = Read.from(sgm.getVariableNames()).toMap(Formatter::display, sgm::getIndex);
		Map<String, Integer> cbm_ = Read.from2(sgm_).mapValue(v -> cbm.getIndex(ne.env.refs[v])).toMap();

		return new Match() {
			public Map<String, Node> apply(Node node) {
				Env env = cbm.env();
				return pred.test(new BindEnv(env), node) ? Read.from2(cbm_).mapValue(env::get).toMap() : null;
			}

			public Node substitute(Map<String, Node> map_) {
				NodeEnv<Atom> ne = sgs.source();
				Reference[] refs = ne.env.refs;
				for (Entry<String, Node> e : map_.entrySet())
					refs[sgm_.get(e.getKey())].bound(e.getValue());
				return ne.node;
			}
		};

	});

}
