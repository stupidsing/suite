package suite.lp;

import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.node.util.Singleton;

public class Configuration {

	public enum TraceLevel {
		NONE, SIMPLE, DETAIL
	}

	public static class ProverCfg {
		private RuleSet ruleSet;
		private boolean isTrace;
		private Source<Node> source;
		private Sink<Node> sink;

		public ProverCfg() {
			this(Suite.newRuleSet());
		}

		public ProverCfg(RuleSet ruleSet) {
			this(ruleSet, Suite.isProverTrace);
		}

		public ProverCfg(RuleSet ruleSet, ProverCfg proverCfg) {
			this(ruleSet, proverCfg.isTrace);
		}

		private ProverCfg(RuleSet ruleSet, boolean isTrace) {
			this.ruleSet = ruleSet;
			this.isTrace = isTrace;
		}

		@Override
		public boolean equals(Object object) {
			return Singleton.me.inspect.equals(this, object);
		}

		@Override
		public int hashCode() {
			return Singleton.me.inspect.hashCode(this);
		}

		public RuleSet ruleSet() {
			return ruleSet;
		}

		public void setRuleSet(RuleSet ruleSet) {
			this.ruleSet = ruleSet;
		}

		public boolean isTrace() {
			return isTrace;
		}

		public void setTrace(boolean isTrace) {
			this.isTrace = isTrace;
		}

		public Source<Node> getSource() {
			return source;
		}

		public void setSource(Source<Node> source) {
			this.source = source;
		}

		public Sink<Node> getSink() {
			return sink;
		}

		public void setSink(Sink<Node> sink) {
			this.sink = sink;
		}
	}

}
