package suite;

import java.util.ArrayList;
import java.util.List;

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
import suite.primitive.Ints_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.To;

public class BindArrayUtil {

	public interface Match {
		public Node[] apply(Node node);

		public Node substitute(Node... nodes);
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
		List<Atom> atoms = new ArrayList<>();
		Atom atom;
		int n = 0;

		while (sgm.getIndex(atom = Atom.of("." + n++)) != null)
			atoms.add(atom);

		int size = atoms.size();
		int[] sgi = Ints_.toArray(size, i -> sgm.getIndex(atoms.get(i)));
		int[] cbi = Ints_.toArray(size, i -> cbm.getIndex(ne.env.refs[sgi[i]]));

		return new Match() {
			public Node[] apply(Node node) {
				Env env = cbm.env();
				return pred.test(new BindEnv(env), node) //
						? To.array(size, Node.class, i -> env.get(cbi[i])) //
						: null;

			}

			public Node substitute(Node... nodes) {
				NodeEnv<Atom> ne = sgs.source();
				Reference[] refs = ne.env.refs;
				for (int i = 0; i < nodes.length; i++)
					refs[sgi[i]].bound(nodes[i]);
				return ne.node;
			}
		};
	});

}
