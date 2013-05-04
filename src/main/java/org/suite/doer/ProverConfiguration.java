package org.suite.doer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.suite.SuiteUtil;
import org.suite.kb.RuleSet;

public class ProverConfiguration {

	private RuleSet ruleSet;
	private boolean isEnableTrace = SuiteUtil.isTrace;
	private Set<String> noTracePredicates = new HashSet<>(Arrays.asList(
			"member", "replace"));

	public ProverConfiguration() {
	}

	public ProverConfiguration(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	public RuleSet ruleSet() {
		return ruleSet;
	}

	public boolean isEnableTrace() {
		return isEnableTrace;
	}

	public void setEnableTrace(boolean isEnableTrace) {
		this.isEnableTrace = isEnableTrace;
	}

	public Set<String> getNoTracePredicates() {
		return noTracePredicates;
	}

	public void setNoTracePredicates(Set<String> noTracePredicates) {
		this.noTracePredicates = noTracePredicates;
	}

}
