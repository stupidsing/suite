package suite.lp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.classpath.Handler;
import suite.immutable.IList;
import suite.lp.checker.Checker;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.util.Rethrow;
import suite.util.To;

public class ImportUtil {

	static {
		Handler.register();
	}

	private String root = "file:" + FileUtil.homeDir() + "/src/main/ll/";
	private ThreadLocal<IList<Node>> importing = ThreadLocal.withInitial(() -> IList.end());

	public Prover newProver(List<String> toImports) {
		return new Prover(newRuleSet(toImports));
	}

	public RuleSet newRuleSet(List<String> toImports) {
		return Rethrow.ioException(() -> {
			RuleSet rs = newRuleSet();
			for (String toImport : toImports)
				importPath(rs, toImport);
			return rs;
		});
	}

	public RuleSet newRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	public boolean importPath(RuleSet rs, String path) throws IOException {
		return importUrl(rs, new URL(root + path));
	}

	public boolean importUrl(RuleSet rs, URL url) throws IOException {
		return importFrom(rs, Suite.parse(To.string(url.openStream())));
	}

	public synchronized boolean importFrom(RuleSet ruleSet, Node node) {
		List<Rule> rules = new ArrayList<>();

		for (Node elem : Tree.iter(node, TermOp.NEXT__))
			rules.add(Rule.formRule(elem));

		Prover prover = new Prover(ruleSet);
		boolean result = true;
		IList<Node> importing0 = importing.get();

		try {
			importing.set(IList.cons(node, importing0));
			for (Rule rule : rules)
				if (rule.head != Atom.NIL)
					ruleSet.addRule(rule);
				else {
					Node goal = SewingGeneralizerImpl.generalize(rule.tail);
					result &= prover.prove(goal);
				}
		} finally {
			importing.set(importing0);
		}

		if (importing0.isEmpty()) // check after all files are imported
			new Checker().check(ruleSet.getRules());

		return result;
	}

}
