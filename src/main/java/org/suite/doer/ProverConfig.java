package org.suite.doer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.suite.SuiteUtil;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;

public class ProverConfig {

	private RuleSet ruleSet;
	private boolean isTrace;
	private Set<String> noTracePredicates;

	public ProverConfig() {
		this(RuleSetUtil.create());
	}

	public ProverConfig(RuleSet ruleSet) {
		this(ruleSet //
				, SuiteUtil.isTrace //
				, new HashSet<>(Arrays.asList("member", "replace")));
	}

	public ProverConfig(ProverConfig proverConfig) {
		this(proverConfig.ruleSet, proverConfig);
	}

	public ProverConfig(RuleSet ruleSet, ProverConfig proverConfig) {
		this(ruleSet, proverConfig.isTrace, proverConfig.noTracePredicates);
	}

	public ProverConfig(RuleSet ruleSet //
			, boolean isTrace //
			, Set<String> noTracePredicates) {
		this.ruleSet = ruleSet;
		this.isTrace = isTrace;
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

	public Set<String> getNoTracePredicates() {
		return noTracePredicates;
	}

	public void setNoTracePredicates(Set<String> noTracePredicates) {
		this.noTracePredicates = noTracePredicates;
	}

}
