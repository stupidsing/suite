package suite.lp.sewing;

import suite.node.Node;

public interface SewingCloner extends VariableMapper {

	public interface Clone_ {
		public Node apply(Env env);
	}

	public Clone_ compile(Node node);

}
