package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface GeneralizerFactory {

	public Env env();

	public Fun<Env, Node> compile(Node node);

}
