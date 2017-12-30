package suite.lp.doer;

import java.util.function.Predicate;

import suite.lp.Configuration.ProverConfig;
import suite.node.Node;

public interface ProverFactory {

	public Predicate<ProverConfig> compile(Node node);

}
