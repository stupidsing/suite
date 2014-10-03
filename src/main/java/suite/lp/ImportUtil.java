package suite.lp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.classpath.Handler;
import suite.lp.doer.Checker;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FileUtil;
import suite.util.To;

public class ImportUtil {

	static {
		Handler.register();
	}

	private String root = "file:" + FileUtil.homeDir() + "/src/main/ll/";

	public Prover createProver(List<String> toImports) {
		return new Prover(createRuleSet(toImports));
	}

	public RuleSet createRuleSet(List<String> toImports) {
		RuleSet rs = createRuleSet();
		try {
			for (String toImport : toImports)
				importPath(rs, toImport);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return rs;
	}

	public RuleSet createRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	public boolean importPath(RuleSet rs, String path) throws IOException {
		return importUrl(rs, new URL(root + path));
	}

	public boolean importUrl(RuleSet rs, URL url1) throws IOException {
		return importFrom(rs, Suite.parse(To.string(url1.openStream())));
	}

	public synchronized boolean importFrom(RuleSet ruleSet, Node node) {
		List<Rule> rules = new ArrayList<>();

		for (Node elem : Tree.iter(node, TermOp.NEXT__))
			rules.add(Rule.formRule(elem));

		new Checker().check(rules);

		Prover prover = new Prover(ruleSet);
		boolean result = true;

		for (Rule rule : rules)
			if (rule.head != Atom.NIL)
				ruleSet.addRule(rule);
			else {
				Node goal = SewingGeneralizer.generalize(rule.tail);
				result &= prover.prove(goal);
			}

		return result;
	}

}
