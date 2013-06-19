package org.suite;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.doer.ProverConfig.TraceLevel;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.Prototype;
import org.suite.kb.Rule;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.search.ProverBuilder.Builder;

public class Suite {

	// Compilation defaults
	public static final boolean isTrace = false;
	public static final boolean isDumpCode = false;
	public static final List<String> libraries = Arrays.asList("STANDARD");
	public static final TraceLevel traceLevel = TraceLevel.SIMPLE;

	private static CompileUtil compileUtil = new CompileUtil();
	private static EvaluateUtil evaluateUtil = new EvaluateUtil();
	private static ImportUtil importUtil = new ImportUtil();
	private static ParseUtil parseUtil = new ParseUtil();

	public static void addRule(RuleSet rs, String rule) {
		addRule(rs, Suite.parse(rule));
	}

	public static void addRule(RuleSet rs, Node node) {
		rs.addRule(Rule.formRule(node));
	}

	public static Node applyFilter(Node func) {
		return Suite.substitute("source {} | .0", func);
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
		RuleSet rs = RuleSetUtil.create();
		RuleSetUtil.importFrom(rs, node);
		return rs;
	}

	/**
	 * Convert rules in a rule set back into to #-separated format.
	 * 
	 * May specify a prototype to limit the rules listed.
	 */
	public static Node getRuleList(RuleSet rs, Prototype proto) {
		List<Node> nodes = new ArrayList<>();

		for (Rule rule : rs.getRules()) {
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

	public static RuleSet logicCompilerRuleSet() {
		return compileUtil.logicCompilerRuleSet();
	}

	public static RuleSet funCompilerRuleSet() {
		return compileUtil.funCompilerRuleSet();
	}

	public static RuleSet funCompilerRuleSet(boolean isLazy) {
		return compileUtil.funCompilerRuleSet(isLazy);
	}

	public static boolean precompile(String libraryName, ProverConfig proverConfig) {
		return compileUtil.precompile(libraryName, proverConfig);
	}

	// --------------------------------
	// Evaluation utilities

	public static boolean proveLogic(String lps) {
		return proveLogic(Suite.parse(lps));
	}

	public static boolean proveLogic(RuleSet rs, String gs) {
		return evaluateUtil.proveLogic(rs, Suite.parse(gs));
	}

	public static boolean proveLogic(Node lp) {
		return evaluateUtil.proveLogic(lp);
	}

	public static List<Node> evaluateLogic(Builder builder, RuleSet rs, Node lp) {
		return evaluateUtil.evaluateLogic(builder, rs, lp);
	}

	public static Node evaluateFun(String fp, boolean isLazy) {
		return evaluateFun(Suite.fcc(Suite.parse(fp), isLazy));
	}

	public static Node evaluateFun(FunCompilerConfig fcc) {
		return evaluateUtil.evaluateFun(fcc);
	}

	public static void evaluateFunIo(FunCompilerConfig fcc, Reader reader, Writer writer) throws IOException {
		evaluateUtil.evaluateFunIo(fcc, reader, writer);
	}

	public static Node evaluateFunType(String fps) {
		return evaluateFunType(Suite.fcc(Suite.parse(fps)));
	}

	public static Node evaluateFunType(FunCompilerConfig fcc) {
		return evaluateUtil.evaluateFunType(fcc);
	}

	// --------------------------------
	// Import utilities

	public static boolean importFrom(RuleSet rs, String name) throws IOException {
		return importUtil.importFrom(rs, name);
	}

	public static boolean importFile(RuleSet rs, String filename) throws IOException {
		return importUtil.importFile(rs, filename);
	}

	public static boolean importResource(RuleSet rs, String classpath) throws IOException {
		return importUtil.importResource(rs, classpath);
	}

	public static Prover createProver(List<String> toImports) {
		return importUtil.createProver(toImports);
	}

	public static RuleSet createRuleSet(List<String> toImports) {
		return importUtil.createRuleSet(toImports);
	}

	// --------------------------------
	// Parse utilities

	public static Node parse(String s) {
		return parseUtil.parse(s);
	}

	public static Node parse(InputStream is) throws IOException {
		return parseUtil.parse(is);
	}

}
