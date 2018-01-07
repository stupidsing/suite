package suite.lp.doer;

import suite.lp.sewing.Env;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface ClonerFactory {

	public interface Clone_ extends Fun<Env, Node> {
	}

	public Env env();

	public Clone_ cloner(Node node);

}
