package suite;

import java.util.Map;

import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Node;
import suite.node.io.Formatter;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.util.Memoize;

public class BindMapUtil {

	public interface Pattern {
		public Map<String, Node> match(Node node);

		public Node subst(Map<String, Node> map);
	}

	public Pattern pattern(String pattern) {
		return patterns.apply(pattern);
	}

	private Fun<String, Pattern> patterns = Memoize.fun(pattern_ -> {
		var sg = new SewingGeneralizerImpl();
		var sgs = sg.g(Suite.parse(pattern_));
		var ne = sgs.g();

		var cb = new CompileBinderImpl(false);
		var pred = cb.binder(ne.node);

		var sgm = sg.mapper();
		var cbm = cb.mapper();
		var sgm_ = Read.from(sgm.getVariableNames()).toMap(Formatter::display, sgm::getIndex);
		var cbm_ = Read.from2(sgm_).mapValue(v -> cbm.getIndex(ne.env.refs[v])).toMap();

		return new Pattern() {
			public Map<String, Node> match(Node node) {
				var env = cbm.env();
				Fun<Integer, Node> envGet = env::get;
				return pred.test(new BindEnv(env), node) ? Read.from2(cbm_).mapValue(envGet).toMap() : null;
			}

			public Node subst(Map<String, Node> map_) {
				var ne = sgs.g();
				var refs = ne.env.refs;
				for (var e : map_.entrySet())
					refs[sgm_.get(e.getKey())].bound(e.getValue());
				return ne.node;
			}
		};

	});

}
