package suite.lp.sewing;

import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface SewingCloner extends VariableMapper {

	public Fun<Env, Node> compile(Node node);

}
