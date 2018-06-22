package suite.lp.kb;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.classpath.Handler;
import suite.immutable.IList;
import suite.lp.check.CheckLogic;
import suite.lp.doer.Prover;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.util.Rethrow;
import suite.util.To;

public interface RuleSet {

	public void addRule(Rule rule);

	public void addRuleToFront(Rule rule);

	public void removeRule(Rule rule);

	public List<Rule> searchRule(Node node);

	public List<Rule> getRules();

	public default boolean importFile(String filename) throws IOException {
		return importUrl(new URL("file", null, filename));
	}

	public default boolean importPath(String path) throws IOException {
		return importUrl(To.url(RuleSetImport.me.root + path));
	}

	public default boolean importUrl(String url) throws IOException {
		return importUrl(To.url(url));
	}

	public default boolean importUrl(URL url) throws IOException {
		return importFrom(Suite.parse(To.string(url.openStream())));
	}

	public default boolean importFrom(Node node) {
		return RuleSetImport.me.importFrom(this, node);
	}

}

class RuleSetImport {

	static RuleSetImport me = new RuleSetImport();

	static {
		Handler.register();
	}

	String root = "file:" + FileUtil.homeDir() + "/src/main/ll/";
	ThreadLocal<IList<Node>> importing = ThreadLocal.withInitial(() -> IList.end());

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
				else
					b &= prover.prove(SewingGeneralizerImpl.generalize(rule.tail));
		} finally {
			importing.set(importing0);
		}

		if (importing0.isEmpty()) // check after all files are imported
			new CheckLogic().check(ruleSet.getRules());

		return b;
	}

}
