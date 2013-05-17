package org.suite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.instructionexecutor.FunInstructionExecutor;
import org.instructionexecutor.LogicInstructionExecutor;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfiguration;
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
import org.util.IoUtil;
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
		Node node = parse(s);
		node = new Generalizer().generalize(node);
		Prover prover = new Prover(rs);
		return prover.prove(node);
	}

	public static boolean evaluateLogical(String program) {
		return evaluateLogical(parse(program));
	}

	public static boolean evaluateLogical(Node program) {
		ProverConfiguration pc = new ProverConfiguration();
		return !evaluateLogical(program, Atom.NIL, pc, false).isEmpty();
	}

	public static List<Node> evaluateLogical(Node program //
			, Node eval //
			, ProverConfiguration pc //
			, boolean isDumpCode) {
		final List<Node> nodes = new ArrayList<>();
		evaluateLogical(program, eval, pc, isDumpCode, new Sink<Node>() {
			public void apply(Node node) {
				nodes.add(node);
			}
		});
		return nodes;
	}

	public static void evaluateLogical(Node program //
			, Node eval //
			, ProverConfiguration pc //
			, boolean isDumpCode //
			, Sink<Node> sink) {
		RuleSet rs = createLogicalCompiler();
		Prover lc = new Prover(new ProverConfiguration(rs, pc));
		Reference code = new Reference();

		String goal = "compile-logic (.0, sink .1) .2"
				+ (isDumpCode ? ", pretty.print .2" : "");
		Node node = SuiteUtil.substitute(goal, program, eval, code);

		if (lc.prove(node))
			new LogicInstructionExecutor(code, lc, sink).execute();
		else
			throw new RuntimeException("Logic compilation error");
	}

	public static class FunCompilerConfig {
		private Node node;
		private boolean isLazy;
		private List<String> libraries = new ArrayList<>();
		private ProverConfiguration proverConfiguration = new ProverConfiguration();
		private boolean isDumpCode = SuiteUtil.isDumpCode;
		private Reader in = new InputStreamReader(System.in, IoUtil.charset);
		private Writer out = new OutputStreamWriter(System.out, IoUtil.charset);

		public FunCompilerConfig() {
			if (SuiteUtil.libraries != null)
				addLibraries(SuiteUtil.libraries);
		}

		public void addLibrary(String library) {
			libraries.add(library);
		}

		public void addLibraries(List<String> libs) {
			libraries.addAll(libs);
		}

		public void setNode(Node node) {
			this.node = node;
		}

		public void setLazy(boolean isLazy) {
			this.isLazy = isLazy;
		}

		public void setLibraries(List<String> libraries) {
			this.libraries = libraries;
		}

		public void setProverConfiguration(
				ProverConfiguration proverConfiguration) {
			this.proverConfiguration = proverConfiguration;
		}

		public void setDumpCode(boolean isDumpCode) {
			this.isDumpCode = isDumpCode;
		}

		public void setIn(Reader in) {
			this.in = in;
		}

		public void setOut(Writer out) {
			this.out = out;
		}
	}

	public static FunCompilerConfig fcc(Node node) {
		return fcc(node, false);
	}

	public static FunCompilerConfig fcc(String program, boolean isLazy) {
		return fcc(parse(program), isLazy);
	}

	public static FunCompilerConfig fcc(Node node, boolean isLazy) {
		FunCompilerConfig c = new FunCompilerConfig();
		c.setNode(node);
		c.setLazy(isLazy);
		return c;
	}

	public static Node evaluateEagerFun(String program) {
		return evaluateFun(program, false);
	}

	public static Node evaluateLazyFun(String program) {
		return evaluateFun(program, true);
	}

	public static Node evaluateFun(String program, boolean isLazy) {
		return evaluateFun(fcc(program, isLazy));
	}

	public static Node evaluateFun(FunCompilerConfig config) {
		RuleSet rs = config.isLazy ? createLazyFunCompiler()
				: createEagerFunCompiler();
		ProverConfiguration pc = config.proverConfiguration;
		Prover compiler = new Prover(new ProverConfiguration(rs, pc));

		Atom mode = Atom.create(config.isLazy ? "LAZY" : "EAGER");
		String program = appendLibraries(config, ".1");
		Reference code = new Reference();

		String s = "compile-function .0 (" + program + ") .2"
				+ (config.isDumpCode ? ", pretty.print .2" : "");
		Node node = SuiteUtil.substitute(s, mode, config.node, code);

		if (compiler.prove(node)) {
			FunInstructionExecutor e = new FunInstructionExecutor(code);
			e.setIn(config.in);
			e.setOut(config.out);
			e.setProver(compiler);

			Node result = e.execute();
			if (config.isLazy)
				result = e.unwrap(result);
			return result;
		} else
			throw new RuntimeException("Function compilation error");
	}

	public static Node evaluateFunType(String program) {
		return evaluateFunType(fcc(SuiteUtil.parse(program)));
	}

	public static Node evaluateFunType(FunCompilerConfig config) {
		Prover compiler = new Prover(new ProverConfiguration(
				createEagerFunCompiler(), config.proverConfiguration));

		Reference type = new Reference();

		Node node = SuiteUtil.substitute("" //
				+ "fc-parse (" + appendLibraries(config, ".0") + ") .p" //
				+ ", infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", resolve-types .tr" //
				+ ", fc-parse-type .1 .t" //
		, config.node, type);

		if (compiler.prove(node))
			return type.finalNode();
		else
			throw new RuntimeException("Type inference error");
	}

	private static String appendLibraries(FunCompilerConfig config,
			String variable) {
		StringBuilder sb = new StringBuilder();
		for (String library : config.libraries)
			if (!Util.isBlank(library))
				sb.append("using " + library + " >> ");
		sb.append("(" + variable + ")");
		return sb.toString();
	}

	private static synchronized RuleSet createLogicalCompiler() {
		if (logicalCompiler == null)
			logicalCompiler = createRuleSet(new String[] { "auto.sl", "lc.sl" });
		return logicalCompiler;
	}

	private static synchronized RuleSet createEagerFunCompiler() {
		if (eagerFunCompiler == null) {
			String imports[] = { "auto.sl", "fc.sl", "fc-eager-evaluation.sl" };
			eagerFunCompiler = createRuleSet(imports);
		}
		return eagerFunCompiler;
	}

	private static synchronized RuleSet createLazyFunCompiler() {
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
