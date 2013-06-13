package org.suite.doer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.suite.Suite;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Node;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;

public class ProverConfig {

	private RuleSet ruleSet;
	private boolean isTrace;
	private TraceLevel traceLevel;
	private Set<String> noTracePredicates;
	private Source<Node> source;
	private Sink<Node> sink;

	public enum TraceLevel {
		NONE, LOG, SIMPLE, DETAIL
	}

	public ProverConfig() {
		this(RuleSetUtil.create());
	}

	public ProverConfig(RuleSet ruleSet) {
		this(ruleSet, Suite.isTrace, new HashSet<>(Arrays.asList("member", "replace")));
	}

	public ProverConfig(ProverConfig proverConfig) {
		this(proverConfig.ruleSet, proverConfig);
	}

	public ProverConfig(RuleSet ruleSet, ProverConfig proverConfig) {
		this(ruleSet, proverConfig.isTrace, proverConfig.noTracePredicates);
	}

	public ProverConfig(RuleSet ruleSet, boolean isTrace, Set<String> noTracePredicates) {
		this.ruleSet = ruleSet;
		this.isTrace = isTrace;
		this.traceLevel = TraceLevel.SIMPLE;
		this.noTracePredicates = noTracePredicates;
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
