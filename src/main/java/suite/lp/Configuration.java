package suite.lp;

import suite.Suite;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.node.util.Singleton;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class Configuration {

	public enum TraceLevel {
		NONE, SIMPLE, DETAIL
	}

	public static class ProverConfig {
		private RuleSet ruleSet;
		private boolean isTrace;
		private Source<Node> source;
		private Sink<Node> sink;

		public ProverConfig() {
			this(Suite.newRuleSet());
		}

		public ProverConfig(RuleSet ruleSet) {
			this(ruleSet, Suite.isProverTrace);
		}

		public ProverConfig(RuleSet ruleSet, ProverConfig proverConfig) {
			this(ruleSet, proverConfig.isTrace);
		}

		private ProverConfig(RuleSet ruleSet, boolean isTrace) {
			this.ruleSet = ruleSet;
			this.isTrace = isTrace;
		}

		@Override
		public boolean equals(Object object) {
			return Singleton.me.getInspect().equals(this, object);
		}

		@Override
		public int hashCode() {
			return Singleton.me.getInspect().hashCode(this);
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
