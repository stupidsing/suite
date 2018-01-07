package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface GeneralizerFactory {

	public interface Generalize_ extends Fun<Env, Node> {
	}

	public Env env();

	public Generalize_ generalizer(Node node);

}
