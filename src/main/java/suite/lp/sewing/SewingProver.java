package suite.lp.sewing;

import suite.lp.Configuration.ProverConfig;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public interface SewingProver {

	public Fun<ProverConfig, Boolean> compile(Node node);

}
