package suite;

import java.util.ArrayList;
import java.util.List;

import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.BinderFactory.Bind_;
import suite.lp.sewing.Env;
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
		SewingGeneralizerImpl sg = new SewingGeneralizerImpl();
		Source<NodeEnv<Atom>> source = sg.g(Suite.parse(pattern_));
		NodeEnv<Atom> ne = source.source();

		CompileBinderImpl cb = new CompileBinderImpl(false);
		Bind_ pred = cb.binder(ne.node);

		List<Atom> atoms = new ArrayList<>();
		Atom atom;
		int n = 0;

		while (sg.vm.getIndex(atom = Atom.of("." + n++)) != null)
			atoms.add(atom);

		int size = atoms.size();
		int[] sg_indices = Ints_.toArray(size, i -> sg.vm.getIndex(atoms.get(i)));
		int[] cb_indices = Ints_.toArray(size, i -> cb.vm.getIndex(ne.env.refs[sg_indices[i]]));

		return new Match() {
			public Node[] apply(Node node) {
				Env env = cb.env();
				return pred.test(new BindEnv(env), node) //
						? To.array(size, Node.class, i -> env.get(cb_indices[i])) //
						: null;

			}

			public Node substitute(Node... nodes) {
				NodeEnv<Atom> ne = source.source();
				Reference[] refs = ne.env.refs;
				for (int i = 0; i < nodes.length; i++)
					refs[sg_indices[i]].bound(nodes[i]);
				return ne.node;
			}
		};
	});

}
