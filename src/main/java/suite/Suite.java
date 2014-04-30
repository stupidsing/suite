package suite;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.fp.FunCompilerConfig;
import suite.instructionexecutor.IndexedReader;
import suite.lp.ImportUtil;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.doer.Configuration.TraceLevel;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.parser.IterativeParser;

public class Suite {

	// Compilation defaults
	public static boolean isTrace = false;
	public static boolean isDumpCode = false;
	public static List<String> libraries = Arrays.asList("STANDARD");
	public static TraceLevel traceLevel = TraceLevel.SIMPLE;

	public static Set<String> tracePredicates = null;
	public static Set<String> noTracePredicates = new HashSet<>(Arrays.asList("member", "replace"));

	private static CompileUtil compileUtil = new CompileUtil();
	private static EvaluateUtil evaluateUtil = new EvaluateUtil();
	private static ImportUtil importUtil = new ImportUtil();
	private static IterativeParser parser = new IterativeParser(TermOp.values());

	public static void addRule(RuleSet rs, String rule) {
		addRule(rs, parse(rule));
	}

	public static void addRule(RuleSet rs, Node node) {
		rs.addRule(Rule.formRule(node));
	}

	public static Node applyDo(Node node, Atom returnType) {
		return substitute("(do-of .1 -> number -> .1) of (skip-type-check id) {.0} {0}", node, returnType);
	}

	public static Node applyReader(Reader reader, Node func) {
		Data<IndexedReader> data = new Data<>(new IndexedReader(reader));
		return substitute("source {skip-type-check atom:.0} | .1", data, func);
	}

	public static FunCompilerConfig fcc(Node fp) {
		return fcc(fp, false);
	}

	public static FunCompilerConfig fcc(Node fp, boolean isLazy) {
		FunCompilerConfig fcc = new FunCompilerConfig();
		fcc.setNode(fp);
		fcc.setLazy(isLazy);
		return fcc;
	}

	public static Node ruleSetToNode(RuleSet rs) {
		return getRuleList(rs, null);
	}

	public static RuleSet nodeToRuleSet(Node node) {
		RuleSet rs = createRuleSet();
		importUtil.importFrom(rs, node);
		return rs;
	}

	/**
	 * Convert rules in a rule set back into to #-separated format.
	 * 
	 * May specify a prototype to limit the rules listed.
	 */
	public static Node getRuleList(RuleSet rs, Prototype proto) {
		List<Node> list = new ArrayList<>();

		for (Rule rule : rs.getRules())
			if (proto == null || proto.equals(Prototype.get(rule)))
				list.add(Rule.formClause(rule));

		return Tree.list(TermOp.NEXT__, list);
	}

	/**
	 * Forms a string using ASCII codes in a list of number.
	 */
	public static String stringize(Node node) {
		StringBuilder sb = new StringBuilder();

		for (Node elem : Tree.iter(node)) {
			Int i = (Int) elem;
			sb.append((char) i.getNumber());
		}

		return sb.toString();
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

	// --------------------------------
	// Compilation utilities

	public static RuleSet funCompilerRuleSet() {
		return compileUtil.funCompilerRuleSet();
	}

	public static RuleSet imperativeCompilerRuleSet() {
		return compileUtil.imperativeCompilerRuleSet();
	}

	public static RuleSet logicCompilerRuleSet() {
		return compileUtil.logicCompilerRuleSet();
	}

	public static boolean precompile(String libraryName, ProverConfig pc) {
		return compileUtil.precompile(libraryName, pc);
	}

	// --------------------------------
	// Evaluation utilities

	public static List<Node> evaluateLogic(Builder builder, RuleSet rs, String lps) {
		return evaluateUtil.evaluateLogic(builder, rs, parse(lps));
	}

	public static String evaluateFilterFun(String program, boolean isLazy, String in) {
		try (Reader reader = new StringReader(in); Writer writer = new StringWriter()) {
			evaluateFilterFun(program, isLazy, reader, writer);
			return writer.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void evaluateFilterFun(String program, boolean isLazy, Reader reader, Writer writer) {
		try {
			Node node = applyReader(reader, parse(program));
			FunCompilerConfig fcc = fcc(node, isLazy);
			evaluateUtil.evaluateFunToWriter(fcc, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Node evaluateFun(String fp, boolean isLazy) {
		return evaluateUtil.evaluateFun(fcc(parse(fp), isLazy));
	}

	public static Node evaluateFun(FunCompilerConfig fcc) {
		return evaluateUtil.evaluateFun(fcc);
	}

	public static void evaluateFunToWriter(FunCompilerConfig fcc, Writer writer) throws IOException {
		evaluateUtil.evaluateFunToWriter(fcc, writer);
	}

	public static Node evaluateFunType(String fps) {
		return evaluateUtil.evaluateFunType(fcc(parse(fps)));
	}

	public static Node evaluateFunType(FunCompilerConfig fcc) {
		return evaluateUtil.evaluateFunType(fcc);
	}

	public static boolean proveLogic(String lps) {
		return evaluateUtil.proveLogic(parse(lps));
	}

	public static boolean proveLogic(RuleSet rs, String lps) {
		return evaluateUtil.proveLogic(rs, parse(lps));
	}

	public static boolean proveLogic(Builder builder, RuleSet rs, String lps) {
		return evaluateUtil.proveLogic(builder, rs, parse(lps));
	}

	public static boolean proveLogic(Builder builder, RuleSet rs, Node lp) {
		return evaluateUtil.proveLogic(builder, rs, lp);
	}

	// --------------------------------
	// Import utilities

	public static Prover createProver(List<String> toImports) {
		return importUtil.createProver(toImports);
	}

	public static RuleSet createRuleSet(List<String> toImports) {
		return importUtil.createRuleSet(toImports);
	}

	public static RuleSet createRuleSet() {
		return importUtil.createRuleSet();
	}

	public static boolean importFile(RuleSet rs, String filename) throws IOException {
		return importUtil.importUrl(rs, new URL("file", null, filename));
	}

	public static boolean importFrom(RuleSet ruleSet, Node node) {
		return importUtil.importFrom(ruleSet, node);
	}

	public static boolean importPath(RuleSet ruleSet, String path) throws IOException {
		return importUtil.importPath(ruleSet, path);
	}

	public static boolean importResource(RuleSet rs, String classpath) throws IOException {
		return importUtil.importUrl(rs, new URL("classpath", null, classpath));
	}

	// --------------------------------
	// Parse utilities

	public static Node parse(String s) {
		return parser.parse(s);
	}

}
