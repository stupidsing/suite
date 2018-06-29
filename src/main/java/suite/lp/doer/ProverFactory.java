package suite.lp.doer;

import suite.lp.Configuration.ProverCfg;
import suite.node.Node;

public interface ProverFactory {

	public Prove_ prover(Node node);

	public interface Prove_ {
		public boolean test(ProverCfg pc);
	}

}
