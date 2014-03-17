package suite.lp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.classpath.Handler;
import suite.lp.doer.Checker;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.To;

public class ImportUtil {

	static {
		Handler.register();
	}

	// The directory of the file we are now importing
	private URL root;

	public ImportUtil() {
		try {
			root = new URL("classpath:");
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public boolean importFrom(RuleSet rs, String path) throws IOException {
		return importPath(rs, path);
	}

	public boolean importFile(RuleSet rs, String path) throws IOException {
		return importUrl(rs, new URL("file", null, path));
	}

	public boolean importResource(RuleSet rs, String path) throws IOException {
		return importUrl(rs, new URL("classpath", null, path));
	}

	public boolean importPath(RuleSet rs, String path) throws IOException {
		return importUrl(rs, rebase(root, path));
	}

	public synchronized boolean importFrom(RuleSet ruleSet, Node node) {
		List<Rule> rules = new ArrayList<>();

		for (Node elem : Tree.iter(node, TermOp.NEXT__))
			rules.add(Rule.formRule(elem));

		new Checker().check(rules);

		Prover prover = new Prover(ruleSet);
		boolean result = true;

		for (Rule rule : rules)
			if (rule.getHead() != Atom.NIL)
				ruleSet.addRule(rule);
			else {
				Node goal = new Generalizer().generalize(rule.getTail());
				result &= prover.prove(goal);
			}

		return result;
	}

	public Prover createProver(List<String> toImports) {
		return new Prover(createRuleSet(toImports));
	}

	public RuleSet createRuleSet(List<String> toImports) {
		RuleSet rs = createRuleSet();
		try {
			for (String toImport : toImports)
				importFrom(rs, toImport);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return rs;
	}

	public RuleSet createRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	private URL rebase(URL root0, String path) throws MalformedURLException {
		String protocol0 = root0.getProtocol();
		String host0 = root0.getHost();
		String path0 = root0.getPath();
		URL url;

		if (!isContainsProtocol(path) && !path.startsWith("/"))
			url = new URL(protocol0, host0, path0 + path);
		else
			url = new URL(path);
		return url;
	}

	private boolean importUrl(RuleSet rs, URL url1) throws MalformedURLException, IOException {
		URL root0 = this.root;
		setRoot(url1);
		try {
			return importFrom(rs, Suite.parse(To.string(url1.openStream())));
		} finally {
			root = root0;
		}
	}

	private void setRoot(URL url) throws MalformedURLException {
		String path1 = url.getPath();
		int pos = path1.lastIndexOf("/");
		root = new URL(url.getProtocol(), url.getHost(), pos >= 0 ? path1.substring(0, pos + 1) : "");
	}

	private boolean isContainsProtocol(String path) {
		int pos0 = path.indexOf(":");
		int pos1 = path.indexOf("/");
		boolean isContainsProtocol = (pos0 <= 0 ? 0 : pos0) >= (pos1 <= 0 ? Integer.MAX_VALUE : pos1);
		return isContainsProtocol;
	}

}
