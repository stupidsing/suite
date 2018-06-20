package suite.lp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.classpath.Handler;
import suite.immutable.IList;
import suite.lp.check.CheckLogic;
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
		return Rethrow.ex(() -> {
			var rs = newRuleSet();
			for (var toImport : toImports)
				importPath(rs, toImport);
			return rs;
		});
	}

	public RuleSet newRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	public boolean importPath(RuleSet rs, String path) throws IOException {
		return importUrl(rs, To.url(root + path));
	}

	public boolean importUrl(RuleSet rs, URL url) throws IOException {
		return importFrom(rs, Suite.parse(To.string(url.openStream())));
	}

	public synchronized boolean importFrom(RuleSet ruleSet, Node node) {
		var rules = new ArrayList<Rule>();

		for (var elem : Tree.iter(node, TermOp.NEXT__))
			rules.add(Rule.of(elem));

		var prover = new Prover(ruleSet);
		var b = true;
		var importing0 = importing.get();

		try {
			importing.set(IList.cons(node, importing0));
			for (var rule : rules)
				if (rule.head != Atom.NIL)
					ruleSet.addRule(rule);
				else {
					var goal = SewingGeneralizerImpl.generalize(rule.tail);
					b &= prover.prove(goal);
				}
		} finally {
			importing.set(importing0);
		}

		if (importing0.isEmpty()) // check after all files are imported
			new CheckLogic().check(ruleSet.getRules());

		return b;
	}

}
