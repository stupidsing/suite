package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Node;

public interface GeneralizerFactory {

	public VariableMapper<Atom> mapper();

	public Generalize_ generalizer(Node node);

	public interface Generalize_ {
		public Node apply(Env env);
	}

}
