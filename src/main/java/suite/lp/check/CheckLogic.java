package suite.lp.check;

import suite.lp.kb.Rule;

import java.util.List;

/**
 * Check logic rules for typical errors.
 */
public class CheckLogic {

	public void check(List<Rule> rules) {
		new CheckPredicateCall().check(rules);
		new CheckSingletonVariable().check(rules);
		if (Boolean.FALSE)
			new CheckType().check(rules);
	}

}
