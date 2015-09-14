package suite;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import suite.fp.FunCompilerConfig;
import suite.fp.intrinsic.Intrinsics;
import suite.immutable.IPointer;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Configuration.TraceLevel;
import suite.lp.ImportUtil;
import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingBinderImpl;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.VariableMapperImpl.Generalization;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.parser.IterativeParser;
import suite.primitive.Chars;
import suite.primitive.IoSink;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Suite {

	// Compilation defaults
	public static boolean isProverTrace = false;
	public static boolean isInstructionDump = false;
	public static boolean isInstructionTrace = false;
	public static int stackSize = 16384;

	public static List<String> libraries = Arrays.asList("STANDARD", "CHARS", "TEXT");
	public static TraceLevel traceLevel = TraceLevel.SIMPLE;

	public static Set<String> tracePredicates = null;
	public static Set<String> noTracePredicates = new HashSet<>(Arrays.asList( //
			"member", //
			"rbt-compare", //
			"rbt-get", //
			"replace"));

	private static CompileUtil compileUtil = new CompileUtil();
	private static EvaluateUtil evaluateUtil = new EvaluateUtil();
	private static ImportUtil importUtil = new ImportUtil();
	private static IterativeParser parser = new IterativeParser(TermOp.values());

	private static Map<String, Fun<Node, Node[]>> matchers = new ConcurrentHashMap<>();

	public static void addRule(RuleSet rs, String rule) {
		rs.addRule(Rule.formRule(parse(rule)));
	}

	public static Node applyStringReader(Node func, Reader reader) {
		return applyCharsReader(substitute(".0 . concat . map {cs-to-string}", func), reader);
	}

	public static Node applyCharsReader(Node func, Reader reader) {
		Data<IPointer<Chars>> data = new Data<>(Intrinsics.read(reader));
		return substitute("atom:.0 | erase-type | cs-drain | .1", data, func);
	}

	public static Node applyPerform(Node func, Node returnType) {
		return substitute("(Do^.1 -> any -> .1) of erase-type {.0} {atom:.2}", func, returnType, Atom.temp());
	}

	public static Node applyWriter(Node func) {
		return Suite.substitute(".0 | lines | map {cs-from-string}", func);
	}

	public static <T> T applyNoLibraries(Source<T> source) {
		return applyLibraries(Collections.emptyList(), source);
	}

	public static <T> T applyLibraries(List<String> libraries, Source<T> source) {
		List<String> libraries0 = Suite.libraries;
		Suite.libraries = libraries;
		try {
			return source.source();
		} finally {
			Suite.libraries = libraries0;
		}
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
		List<Node> nodes = Read.from(rs.getRules()) //
				.filter(rule -> proto == null || proto.equals(Prototype.of(rule))) //
				.map(Rule::formClause) //
				.toList();
		return Tree.of(TermOp.NEXT__, nodes);
	}

	public static Fun<Node, Node[]> matcher(String s) {
		return matchers.computeIfAbsent(s, s_ -> {
			Generalizer generalizer = new Generalizer();
			Node toMatch = generalizer.generalize(parse(s_));

			SewingBinderImpl sb = new SewingBinderImpl(false);
			BiPredicate<BindEnv, Node> pred = sb.compileBind(toMatch);
			List<Integer> indexList = new ArrayList<>();
			Integer index;
			int n = 0;
			while ((index = sb.getVariableIndex(generalizer.getVariable(Atom.of("." + n++)))) != null)
				indexList.add(index);

			int size = indexList.size();
			int indices[] = new int[size];
			for (int i = 0; i < size; i++)
				indices[i] = indexList.get(i);

			return node -> {
				Env env = sb.env();
				Trail trail = new Trail();
				BindEnv be = new BindEnv() {
					public Env getEnv() {
						return env;
					}

					public Trail getTrail() {
						return trail;
					}
				};
				if (pred.test(be, node)) {
					List<Node> results = new ArrayList<>(size);
					for (int i = 0; i < size; i++)
						results.add(env.get(indices[i]));
					return results.toArray(new Node[size]);
				} else
					return null;
			};
		});
	}

	public static Node substitute(String s, Node... nodes) {
		Generalization generalization = SewingGeneralizerImpl.process(parse(s));
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

	public static Source<Node> evaluateLogic(Builder builder, RuleSet rs, String lps) {
		return evaluateUtil.evaluateLogic(builder, rs, parse(lps));
	}

	public static String evaluateFilterFun(String program, String in, boolean isLazy, boolean isDo) {
		try (Reader reader = new StringReader(in); Writer writer = new StringWriter()) {
			evaluateFilterFun(program, reader, writer, isLazy, isDo);
			return writer.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void evaluateFilterFun(String program, Reader reader, Writer writer, boolean isLazy, boolean isDo) {
		try {
			Node node0 = parse(program);
			Node node1 = applyStringReader(node0, reader);
			Node node2 = isDo ? Suite.applyPerform(node1, Atom.of("string")) : node1;
			Node node3 = applyWriter(node2);
			evaluateFunToWriter(fcc(node3, isLazy), writer);
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
