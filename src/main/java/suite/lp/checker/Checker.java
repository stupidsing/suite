package suite.lp.checker;

import java.util.List;

import suite.lp.kb.Rule;

/**
 * Check logic rules for typical errors.
 */
public class Checker {

	public void check(List<Rule> rules) {
		new SingletonVariableChecker().check(rules);
	}

}
