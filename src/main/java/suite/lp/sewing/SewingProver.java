package suite.lp.sewing;

import java.util.function.Predicate;

import suite.lp.Configuration.ProverConfig;
import suite.node.Node;

public interface SewingProver {

	public Predicate<ProverConfig> compile(Node node);

}
