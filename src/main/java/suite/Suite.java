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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import suite.fp.FunCompilerConfig;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.instructionexecutor.IndexedCharsReader;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Configuration.TraceLevel;
import suite.lp.ImportUtil;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.sewing.SewingGeneralizer;
import suite.lp.sewing.SewingGeneralizer.Generalization;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.parser.IterativeParser;
import suite.primitive.IoSink;

public class Suite {

	// Compilation defaults
	public static boolean isProverTrace = false;
	public static boolean isInstructionDump = false;
	public static boolean isInstructionTrace = false;
	public static int stackSize = 16384;

	public static List<String> libraries = Arrays.asList("STANDARD", "CHARS");
	public static TraceLevel traceLevel = TraceLevel.SIMPLE;

	public static Set<String> tracePredicates = null;
	public static Set<String> noTracePredicates = new HashSet<>(Arrays.asList("member", "replace"));

	private static CompileUtil compileUtil = new CompileUtil();
	private static EvaluateUtil evaluateUtil = new EvaluateUtil();
	private static ImportUtil importUtil = new ImportUtil();
	private static IterativeParser parser = new IterativeParser(TermOp.values());

	public static void addRule(RuleSet rs, String rule) {
		rs.addRule(Rule.formRule(parse(rule)));
	}

	public static Node applyStringReader(Node func, Reader reader) {
		return applyCharsReader(substitute(".0 . concat . map {cs-to-string}", func), reader);
	}

	public static Node applyCharsReader(Node func, Reader reader) {
		Data<IndexedCharsReader.Pointer> data = new Data<>(new IndexedCharsReader(reader).pointer());
		return substitute("skip-type-check atom:.0 | .1 . cs-drain", data, func);
	}

	public static Node applyPerform(Node func, Node returnType) {
		return substitute("(Do^.1 -> any -> .1) of (skip-type-check id) {.0} {atom:.2}", func, returnType, Atom.temp());
	}

	public static Node applyWriter(Node func) {
		return Suite.substitute(".0 | lines | map {cs-from-string}", func);
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
		List<Node> nodes = rs.getRules().stream() //
				.filter(rule -> proto == null || proto.equals(Prototype.of(rule))) //
				.map(Rule::formClause) //
				.collect(Collectors.toList());
		return Tree.list(TermOp.NEXT__, nodes);
	}

	public static Node[] match(String s, Node node) {
		Generalization generalization = SewingGeneralizer.process(parse(s));

		if (Binder.bind(generalization.node, node, new Journal())) {
			Map<Node, Node> variables = generalization.getVariables();
			List<Node> results = new ArrayList<>();
			int i = 0;
			Node value;
			while ((value = variables.get(Atom.of("." + i++))) != null)
				results.add(value.finalNode());
			return results.toArray(new Node[results.size()]);
		} else
			return null;
	}

	public static Node substitute(String s, Node... nodes) {
		Generalization generalization = SewingGeneralizer.process(parse(s));
		int i = 0;

		for (Node node : nodes) {
			Node variable = generalization.getVariable(Atom.of("." + i++));
			((Reference) variable).bound(node);
		}

		return generalization.node;
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
			Node node = applyWriter(applyStringReader(parse(program), reader));
			evaluateFunToWriter(fcc(node, isLazy), writer);
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
		evaluateUtil.evaluateCallback(fcc, executor -> executor.executeToWriter(writer));
	}

	public static void evaluateCallback(FunCompilerConfig fcc, IoSink<FunInstructionExecutor> sink) throws IOException {
		evaluateUtil.evaluateCallback(fcc, sink);
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
