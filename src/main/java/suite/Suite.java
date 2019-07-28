package suite;

import static suite.util.Fail.fail;
import static suite.util.Rethrow.ex;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.BindArrayUtil.Pattern;
import suite.fp.FunCompilerCfg;
import suite.fp.intrinsic.Intrinsics;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.lp.Configuration.ProverCfg;
import suite.lp.Configuration.TraceLevel;
import suite.lp.doer.Prover;
import suite.lp.kb.DoubleIndexedRuleSet;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.io.TermOp;
import suite.node.parser.IterativeParser;
import suite.node.util.TreeUtil;
import suite.primitive.IoSink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;

public class Suite {

	// compilation defaults
	public static boolean isProverTrace = false;
	public static boolean isInstructionDump = false;
	public static int stackSize = 16384;

	public static List<String> libraries = List.of("STANDARD", "CHARS", "TEXT");
	public static TraceLevel traceLevel = TraceLevel.SIMPLE;

	public static Set<String> tracePredicates = null;
	public static Set<String> noTracePredicates = new HashSet<>(List.of( //
			"member", //
			"rbt-compare", //
			"rbt-get", //
			"replace"));

	private static BindArrayUtil bindArrayUtil = new BindArrayUtil();
	private static CompileUtil compileUtil = new CompileUtil();
	private static EvaluateUtil evaluateUtil = new EvaluateUtil();
	private static IterativeParser parser = new IterativeParser(TermOp.values());

	public static void addRule(RuleSet rs, String rule) {
		rs.addRule(Rule.of(parse(rule)));
	}

	public static Node applyStringReader(Node func, Reader reader) {
		return applyCharsReader(substitute(".0 . concat . map_{cs-to-string}", func), reader);
	}

	public static Node applyCharsReader(Node func, Reader reader) {
		var data = new Data<>(Intrinsics.read(reader));
		return substitute("atom:.0 | erase-type | cs-drain | .1", data, func);
	}

	public static Node applyPerform(Node func, Node returnType) {
		return substitute("(Do^.1 -> any -> .1) of erase-type_{.0}_{atom:.2}", func, returnType, Atom.temp());
	}

	public static Node applyWriter(Node func) {
		return substitute(".0 | lines | map_{cs-from-string}", func);
	}

	public static FunCompilerCfg fcc(Node fp) {
		return fcc(fp, false);
	}

	public static FunCompilerCfg fcc(Node fp, boolean isLazy) {
		var fcc = new FunCompilerCfg();
		fcc.setLazy(isLazy);
		fcc.setNode(fp);
		return fcc;
	}

	public static Node getRules(RuleSet rs) {
		return listRules(rs, null);
	}

	public static RuleSet getRuleSet(Node node) {
		var rs = newRuleSet();
		rs.importFrom(node);
		return rs;
	}

	/**
	 * Convert rules in a rule set back into to #-separated format.
	 *
	 * May specify a prototype to limit the rules listed.
	 */
	public static Node listRules(RuleSet rs, Prototype proto) {
		var nodes = Read //
				.from(rs.getRules()) //
				.filter(rule -> proto == null || proto.equals(Prototype.of(rule))) //
				.map(Rule::clause) //
				.toList();
		return TreeUtil.buildUp(TermOp.NEXT__, nodes);
	}

	public static Prover newProver(List<String> toImports) {
		return new Prover(newRuleSet(toImports));
	}

	public static RuleSet newRuleSet(List<String> toImports) {
		return ex(() -> {
			var rs = newRuleSet();
			for (var toImport : toImports)
				rs.importPath(toImport);
			return rs;
		});
	}

	public static RuleSet newRuleSet() {
		return new DoubleIndexedRuleSet();
	}

	public static <T> T noLibraries(Source<T> source) {
		return useLibraries(List.of(), source);
	}

	public static <T> T useLibraries(List<String> libraries_, Source<T> source) {
		var libraries0 = libraries;
		libraries = libraries_;
		try {
			return source.g();
		} finally {
			libraries = libraries0;
		}
	}

	// --------------------------------
	// bind utilities

	public static Pattern pattern(String pattern) {
		return bindArrayUtil.pattern(pattern);
	}

	public static Node substitute(String pattern, Node... nodes) {
		return bindArrayUtil.pattern(pattern).subst(nodes);
	}

	// --------------------------------
	// compilation utilities

	public static RuleSet funCompilerRuleSet() {
		return compileUtil.funCompilerRuleSet();
	}

	public static RuleSet imperativeCompilerRuleSet() {
		return compileUtil.imperativeCompilerRuleSet();
	}

	public static RuleSet logicCompilerRuleSet() {
		return compileUtil.logicCompilerRuleSet();
	}

	public static boolean precompile(String libraryName, ProverCfg pc) {
		return compileUtil.precompile(libraryName, pc);
	}

	// --------------------------------
	// evaluation utilities

	public static Source<Node> evaluateLogic(Builder builder, RuleSet rs, String lps) {
		return evaluateUtil.evaluateLogic(builder, rs, parse(lps));
	}

	public static String evaluateFilterFun(String program, String in, boolean isLazy, boolean isDo) {
		try (var reader = new StringReader(in); var writer = new StringWriter()) {
			evaluateFilterFun(program, reader, writer, isLazy, isDo);
			return writer.toString();
		} catch (IOException ex) {
			return fail(ex);
		}
	}

	public static void evaluateFilterFun(String program, Reader reader, Writer writer, boolean isLazy, boolean isDo) {
		ex(() -> {
			var node0 = parse(program);
			var node1 = applyStringReader(node0, reader);
			var node2 = isDo ? applyPerform(node1, Atom.of("string")) : node1;
			var node3 = applyWriter(node2);
			evaluateFunToWriter(fcc(node3, isLazy), writer);
			return writer;
		});
	}

	public static Node evaluateFun(String fp, boolean isLazy) {
		return evaluateUtil.evaluateFun(fcc(parse(fp), isLazy));
	}

	public static Node evaluateFun(FunCompilerCfg fcc) {
		return evaluateUtil.evaluateFun(fcc);
	}

	public static void evaluateFunToWriter(FunCompilerCfg fcc, Writer writer) throws IOException {
		evaluateUtil.evaluateCallback(fcc, executor -> executor.executeToWriter(writer));
	}

	public static void evaluateCallback(FunCompilerCfg fcc, IoSink<FunInstructionExecutor> sink) throws IOException {
		evaluateUtil.evaluateCallback(fcc, sink);
	}

	public static Node evaluateFunType(String fps) {
		return evaluateUtil.evaluateFunType(fcc(parse(fps)));
	}

	public static Node evaluateFunType(FunCompilerCfg fcc) {
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
	// parse utilities

	public static Node parse(String s) {
		return parser.parse(s);
	}

}
