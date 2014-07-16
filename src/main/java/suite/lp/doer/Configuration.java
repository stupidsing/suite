package suite.lp.doer;

import java.util.Set;

import suite.Suite;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.node.util.Singleton;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class Configuration {

	public enum TraceLevel {
		NONE, LOG, SIMPLE, DETAIL
	}

	public static class ProverConfig {
		private RuleSet ruleSet;
		private boolean isTrace;
		private TraceLevel traceLevel;
		private Set<String> tracePredicates;
		private Set<String> noTracePredicates;
		private Source<Node> source;
		private Sink<Node> sink;

		public ProverConfig() {
			this(Suite.createRuleSet());
		}

		public ProverConfig(RuleSet ruleSet) {
			this(ruleSet, Suite.isProverTrace, Suite.tracePredicates, Suite.noTracePredicates);
		}

		public ProverConfig(ProverConfig proverConfig) {
			this(proverConfig.ruleSet, proverConfig);
		}

		public ProverConfig(RuleSet ruleSet, ProverConfig proverConfig) {
			this(ruleSet, proverConfig.isTrace, proverConfig.tracePredicates, proverConfig.noTracePredicates);
		}

		private ProverConfig(RuleSet ruleSet, boolean isTrace, Set<String> tracePredicates, Set<String> noTracePredicates) {
			this.ruleSet = ruleSet;
			this.isTrace = isTrace;
			this.traceLevel = Suite.traceLevel;
			this.tracePredicates = tracePredicates;
			this.noTracePredicates = noTracePredicates;
		}

		@Override
		public boolean equals(Object object) {
			return Singleton.get().getInspectUtil().equals(this, object);
		}

		@Override
		public int hashCode() {
			return Singleton.get().getInspectUtil().hashCode(this);
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

		public TraceLevel getTraceLevel() {
			return traceLevel;
		}

		public void setTraceLevel(TraceLevel traceLevel) {
			this.traceLevel = traceLevel;
		}

		public Set<String> getTracePredicates() {
			return tracePredicates;
		}

		public void setTracePredicates(Set<String> tracePredicates) {
			this.tracePredicates = tracePredicates;
		}

		public Set<String> getNoTracePredicates() {
			return noTracePredicates;
		}

		public void setNoTracePredicates(Set<String> noTracePredicates) {
			this.noTracePredicates = noTracePredicates;
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
