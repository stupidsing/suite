package suite;

import java.util.ArrayList;
import java.util.List;

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
		Node node = Suite.parse(pattern_);
		Generalizer generalizer = new Generalizer();

		CompileBinderImpl cb = new CompileBinderImpl(false);
		Bind_ pred = cb.binder(generalizer.generalize(node));

		SewingGeneralizerImpl sg = new SewingGeneralizerImpl();
		Source<NodeEnv<Atom>> source = sg.g(node);

		List<Atom> atoms = new ArrayList<>();
		List<Node> variables = new ArrayList<>();
		Atom atom;
		Node variable;
		int n = 0;

		while (cb.vm.getIndex(variable = generalizer.getVariable(atom = Atom.of("." + n++))) != null) {
			atoms.add(atom);
			variables.add(variable);
		}

		int size = variables.size();
		int[] indices0 = Ints_.toArray(size, i -> cb.vm.getIndex(variables.get(i)));
		int[] indices1 = Ints_.toArray(size, i -> sg.vm.getIndex(atoms.get(i)));

		return new Match() {
			public Node[] apply(Node node) {
				Env env = cb.env();
				return pred.test(new BindEnv(env), node) //
						? To.array(size, Node.class, i -> env.get(indices0[i])) //
						: null;

			}

			public Node substitute(Node... nodes) {
				NodeEnv<Atom> ne = source.source();
				Reference[] refs = ne.env.refs;
				for (int i = 0; i < nodes.length; i++)
					refs[indices1[i]].bound(nodes[i]);
				return ne.node;
			}
		};
	});

}
