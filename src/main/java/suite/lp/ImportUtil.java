package suite.lp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.lp.doer.Checker;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;
import suite.util.To;

public class ImportUtil {

	// The directory of the file we are now importing
	private boolean isImportFromClasspath = false;
	private String importerRoot = "";

	public boolean importFrom(RuleSet rs, String name) throws IOException {
		if (isImportFromClasspath)
			return importResource(rs, name);
		else
			return importFile(rs, name);
	}

	public boolean importFile(RuleSet rs, String filename) throws IOException {
		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		String filename1 = setImporterRoot(false, filename, oldRoot);

		try {
			return importFrom(rs, Suite.parse(To.string(new File(filename1))));
		} finally {
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
	}

	public boolean importResource(RuleSet rs, String classpath) throws IOException {
		ClassLoader cl = Suite.class.getClassLoader();

		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		String classpath1 = setImporterRoot(true, classpath, oldRoot);

		try (InputStream is = cl.getResourceAsStream(classpath1)) {
			if (is != null)
				return importFrom(rs, Suite.parse(To.string(is)));
			else
				throw new RuntimeException("Cannot find resource " + classpath1);
		} finally {
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
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
				importResource(rs, toImport);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return rs;
	}

	public RuleSet createRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	private String setImporterRoot(boolean isFromClasspath, String name, String oldRoot) {
		isImportFromClasspath = isFromClasspath;

		if (!name.startsWith(File.separator))
			name = oldRoot + name;

		int pos = name.lastIndexOf(File.separator);
		importerRoot = pos >= 0 ? name.substring(0, pos + 1) : "";
		return name;
	}

}
