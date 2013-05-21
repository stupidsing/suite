package org.suite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.instructionexecutor.FunInstructionExecutor;
import org.instructionexecutor.LogicInstructionExecutor;
import org.suite.doer.Cloner;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.doer.TermParser;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.Prototype;
import org.suite.kb.Rule;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.util.Util;
import org.util.Util.Sink;

public class SuiteUtil {

	// Compilation defaults
	public static final boolean isTrace = false;
	public static final boolean isDumpCode = false;
	public static final List<String> libraries = Arrays.asList("STANDARD");

	// Compilation objects
	private static TermParser parser = new TermParser();
	private static RuleSet logicalCompiler;
	private static RuleSet eagerFunCompiler;
	private static RuleSet lazyFunCompiler;

	// The directory of the file we are now importing
	private static boolean isImportFromClasspath = false;
	private static String importerRoot = "";

	private static class Collector implements Sink<Node> {
		private List<Node> nodes = new ArrayList<>();

		public void apply(Node node) {
			nodes.add(new Cloner().clone(node));
		}

		public List<Node> getNodes() {
			return nodes;
		}
	}

	public static void addRule(RuleSet rs, String rule) {
		rs.addRule(Rule.formRule(parser.parse(rule)));
	}

	public static synchronized boolean importFrom(RuleSet rs, String name)
			throws IOException {
		if (isImportFromClasspath)
			return SuiteUtil.importResource(rs, name);
		else
			return SuiteUtil.importFile(rs, name);
	}

	public static synchronized boolean importFile(RuleSet rs, String filename)
			throws IOException {
		FileInputStream is = null;

		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		filename = setImporterRoot(false, filename, oldRoot);

		try {
			is = new FileInputStream(filename);
			return RuleSetUtil.importFrom(rs, SuiteUtil.parse(is));
		} finally {
			Util.closeQuietly(is);
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
	}

	public static synchronized boolean importResource(RuleSet rs,
			String classpath) throws IOException {
		ClassLoader cl = SuiteUtil.class.getClassLoader();
		InputStream is = null;

		boolean wasFromClasspath = isImportFromClasspath;
		String oldRoot = importerRoot;
		classpath = setImporterRoot(true, classpath, oldRoot);

		try {
			is = cl.getResourceAsStream(classpath);
			if (is != null)
				return RuleSetUtil.importFrom(rs, SuiteUtil.parse(is));
			else
				throw new RuntimeException("Cannot find resource " + classpath);
		} finally {
			Util.closeQuietly(is);
			isImportFromClasspath = wasFromClasspath;
			importerRoot = oldRoot;
		}
	}

	private static String setImporterRoot(boolean isFromClasspath, String name,
			String oldRoot) {
		isImportFromClasspath = isFromClasspath;

		if (!name.startsWith(File.separator))
			name = oldRoot + name;

		int pos = name.lastIndexOf(File.separator);
		importerRoot = pos >= 0 ? name.substring(0, pos + 1) : "";
		return name;
	}

	public static boolean proveThis(RuleSet rs, String s) {
		return new Prover(rs).prove(new Generalizer().generalize(parse(s)));
	}

	public static boolean evaluateLogical(String lps) {
		return evaluateLogical(parse(lps));
	}

	public static boolean evaluateLogical(Node lp) {
		ProverConfig pc = new ProverConfig();
		return !evaluateLogical(lp, Atom.NIL, pc, false).isEmpty();
	}

	public static List<Node> evaluateLogical(Node lp //
			, Node eval //
			, ProverConfig pc //
			, boolean isDumpCode) {
		Collector sink = new Collector();
		evaluateLogical(lp, eval, pc, isDumpCode, sink);
		return sink.getNodes();
	}

	public static void evaluateLogical(Node lp //
			, Node eval //
			, ProverConfig pc //
			, boolean isDumpCode //
			, Sink<Node> sink) {
		RuleSet rs = logicalRuleSet();
		Prover lc = new Prover(new ProverConfig(rs, pc));
		Reference code = new Reference();

		String goal = "compile-logic (.0, sink .1) .2"
				+ (isDumpCode ? ", pretty.print .2" : "");
		Node node = SuiteUtil.substitute(goal, lp, eval, code);

		if (lc.prove(node))
			new LogicInstructionExecutor(code, lc, sink).execute();
		else
			throw new RuntimeException("Logic compilation error");
	}

	public static FunCompilerConfig fcc(Node fp) {
		return fcc(fp, false);
	}

	public static FunCompilerConfig fcc(String fps, boolean isLazy) {
		return fcc(parse(fps), isLazy);
	}

	public static FunCompilerConfig fcc(Node fp, boolean isLazy) {
		FunCompilerConfig fcc = new FunCompilerConfig();
		fcc.setNode(fp);
		fcc.setLazy(isLazy);
		return fcc;
	}

	public static Node evaluateEagerFun(String fp) {
		return evaluateFun(fp, false);
	}

	public static Node evaluateLazyFun(String fp) {
		return evaluateFun(fp, true);
	}

	private static Node evaluateFun(String fp, boolean isLazy) {
		return evaluateFun(fcc(fp, isLazy));
	}

	public static Node evaluateFun(FunCompilerConfig fcc) {
		RuleSet rs = fcc.isLazy() ? lazyFunRuleSet() : eagerFunRuleSet();
		ProverConfig pc = fcc.getProverConfig();
		Prover compiler = new Prover(new ProverConfig(rs, pc));

		Atom mode = Atom.create(fcc.isLazy() ? "LAZY" : "EAGER");
		String program = appendLibraries(fcc, ".1");
		Reference code = new Reference();

		String s = "compile-function .0 (" + program + ") .2"
				+ (fcc.isDumpCode() ? ", pretty.print .2" : "");
		Node node = SuiteUtil.substitute(s, mode, fcc.getNode(), code);

		if (compiler.prove(node)) {
			FunInstructionExecutor e = new FunInstructionExecutor(code);
			e.setIn(fcc.getIn());
			e.setOut(fcc.getOut());
			e.setProver(compiler);

			Node result = e.execute();
			if (fcc.isLazy())
				result = e.unwrap(result);
			return result;
		} else
			throw new RuntimeException("Function compilation error");
	}

	public static Node evaluateFunType(String fps) {
		return evaluateFunType(fcc(SuiteUtil.parse(fps)));
	}

	public static Node evaluateFunType(FunCompilerConfig fcc) {
		Prover compiler = new Prover(new ProverConfig( //
				eagerFunRuleSet(), fcc.getProverConfig()));

		Reference type = new Reference();

		Node node = SuiteUtil.substitute("" //
				+ "fc-parse (" + appendLibraries(fcc, ".0") + ") .p" //
				+ ", infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", resolve-types .tr" //
				+ ", fc-parse-type .1 .t" //
		, fcc.getNode(), type);

		if (compiler.prove(node))
			return type.finalNode();
		else
			throw new RuntimeException("Type inference error");
	}

	private static String appendLibraries(FunCompilerConfig fcc, String variable) {
		StringBuilder sb = new StringBuilder();
		for (String library : fcc.getLibraries())
			if (!Util.isBlank(library))
				sb.append("using " + library + " >> ");
		sb.append("(" + variable + ")");
		return sb.toString();
	}

	private static synchronized RuleSet logicalRuleSet() {
		if (logicalCompiler == null)
			logicalCompiler = createRuleSet(new String[] { "auto.sl", "lc.sl" });
		return logicalCompiler;
	}

	private static synchronized RuleSet eagerFunRuleSet() {
		if (eagerFunCompiler == null) {
			String imports[] = { "auto.sl", "fc.sl", "fc-eager-evaluation.sl" };
			eagerFunCompiler = createRuleSet(imports);
		}
		return eagerFunCompiler;
	}

	private static synchronized RuleSet lazyFunRuleSet() {
		if (lazyFunCompiler == null) {
			String imports[] = { "auto.sl", "fc.sl", "fc-lazy-evaluation.sl" };
			lazyFunCompiler = createRuleSet(imports);
		}
		return lazyFunCompiler;
	}

	public static Prover createProver(String toImports[]) {
		return new Prover(createRuleSet(toImports));
	}

	public static RuleSet createRuleSet(String toImports[]) {
		RuleSet rs = RuleSetUtil.create();
		try {
			for (String toImport : toImports)
				SuiteUtil.importResource(rs, toImport);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return rs;
	}

	public static Node parse(String s) {
		return parser.parse(s);
	}

	public static Node parse(InputStream is) throws IOException {
		return parser.parse(is);
	}

	/**
	 * Forms a string using ASCII codes in a list of number.
	 */
	public static String stringize(Node node) {
		StringBuilder sb = new StringBuilder();
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			Int i = (Int) tree.getLeft();
			sb.append((char) i.getNumber());
			node = tree.getRight();
		}

		return sb.toString();
	}

	/**
	 * Convert rules in a rule set back into to #-separated format.
	 * 
	 * May specify a prototype to limit the rules listed.
	 */
	public static Node getRuleList(RuleSet ruleSet, Prototype proto) {
		List<Node> nodes = new ArrayList<>();

		for (Rule rule : ruleSet.getRules()) {
			Prototype p1 = Prototype.get(rule);
			if (proto == null || proto.equals(p1)) {
				Node clause = Rule.formClause(rule);
				nodes.add(clause);
			}
		}

		Node node = Atom.NIL;
		for (int i = nodes.size() - 1; i >= 0; i--)
			node = Tree.create(TermOp.NEXT__, nodes.get(i), node);
		return node;
	}

	public static Node substitute(String s, Node... nodes) {
		Node result = parse(s);
		Generalizer generalizer = new Generalizer();
		result = generalizer.generalize(result);
		int i = 0;

		for (Node node : nodes) {
			Node variable = generalizer.getVariable(Atom.create("." + i++));
			((Reference) variable).bound(node);
		}

		return result;
	}

}
