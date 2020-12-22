package suite;

import java.util.ArrayList;

import primal.fp.Funs.Fun;
import primal.primitive.IntVerbs.ToInt;
import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.parser.IterativeParser;
import suite.util.Memoize;
import suite.util.To;

public class BindArrayUtil {

	private IterativeParser parser;

	public interface Pattern {
		public Node[] match(Node node);

		public Node subst(Node... nodes);
	}

	public BindArrayUtil(IterativeParser parser) {
		this.parser = parser;
	}

	public Pattern pattern(String pattern) {
		return patterns.apply(pattern);
	}

	private Fun<String, Pattern> patterns = Memoize.fun(pattern_ -> {
		var sg = new SewingGeneralizerImpl();
		var sgs = sg.g(parser.parse(pattern_));
		var ne = sgs.g();

		var cb = new CompileBinderImpl(false);
		var pred = cb.binder(ne.node);

		var sgm = sg.mapper();
		var cbm = cb.mapper();
		var atoms = new ArrayList<Atom>();
		Atom atom;
		var n = 0;

		while (sgm.getIndex(atom = Atom.of(ProverConstant.variablePrefix + n++)) != null)
			atoms.add(atom);

		var size = atoms.size();
		var sgi = ToInt.array(size, i -> sgm.getIndex(atoms.get(i)));
		var cbi = ToInt.array(size, i -> cbm.getIndex(ne.env.refs[sgi[i]]));

		return new Pattern() {
			public Node[] match(Node node) {
				var env = cbm.env();
				return pred.test(new BindEnv(env), node) //
						? To.array(size, Node.class, i -> env.get(cbi[i])) //
						: null;
			}

			public Node subst(Node... nodes) {
				var ne = sgs.g();
				var refs = ne.env.refs;
				for (var i = 0; i < nodes.length; i++)
					refs[sgi[i]].bound(nodes[i]);
				return ne.node;
			}
		};
	});

}
