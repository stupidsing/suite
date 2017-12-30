package suite.lp.sewing;

import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface SewingGeneralizer {

	public Env env();

	public Fun<Env, Node> compile(Node node);

}
