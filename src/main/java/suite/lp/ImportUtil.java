package suite.lp;

import java.util.List;

import suite.classpath.Handler;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.RuleSet;
import suite.util.Rethrow;

public class ImportUtil {

	static {
		Handler.register();
	}

	public Prover newProver(List<String> toImports) {
		return new Prover(newRuleSet(toImports));
	}

	public RuleSet newRuleSet(List<String> toImports) {
		return Rethrow.ex(() -> {
			var rs = newRuleSet();
			for (var toImport : toImports)
				rs.importPath(toImport);
			return rs;
		});
	}

	public RuleSet newRuleSet() {
		return new DoubleIndexedRuleSet();
	}

}
