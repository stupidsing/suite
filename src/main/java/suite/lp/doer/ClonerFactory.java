package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.node.Node;
import suite.node.Reference;

public interface ClonerFactory {

	public VariableMapper<Reference> mapper();

	public Clone_ cloner(Node node);

	public interface Clone_ {
		public Node apply(Env env);
	}

}
