package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.lp.sewing.VariableMapper.NodeEnv;
import suite.node.Atom;
import suite.node.Node;
import suite.streamlet.FunUtil.Source;

public interface GeneralizerFactory {

	public VariableMapper<Atom> mapper();

	public Generalize_ generalizer(Node node);

	public interface Generalize_ {
		public Node apply(Env env);
	}

	public default Source<NodeEnv<Atom>> g(Node node) {
		return mapper().g(generalizer(node)::apply);
	}

}
