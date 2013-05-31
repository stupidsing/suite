package org.suite;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
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

public class Suite {

	// Compilation defaults
	public static final boolean isTrace = false;
	public static final boolean isDumpCode = false;
	public static final List<String> libraries = Arrays.asList("STANDARD");

	private static SuiteCompileUtil suiteCompileUtil = new SuiteCompileUtil();
	private static SuiteEvaluationUtil suiteEvaluationUtil = new SuiteEvaluationUtil();
	private static SuiteImportUtil suiteImportUtil = new SuiteImportUtil();
	private static SuiteParseUtil suiteParseUtil = new SuiteParseUtil();

	public static void addRule(RuleSet rs, String rule) {
		addRule(rs, Suite.parse(rule));
	}

	public static void addRule(RuleSet rs, Node node) {
		rs.addRule(Rule.formRule(node));
	}

	public static Node applyFilter(Node func) {
		return Suite.substitute("source {} | .0 | sink {}", func);
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

	public static Node ruleSetToNode(RuleSet ruleSet) {
		return getRuleList(ruleSet, null);
	}

	public static RuleSet nodeToRuleSet(Node node) {
		RuleSet ruleSet = RuleSetUtil.create();
		RuleSetUtil.importFrom(ruleSet, node);
		return ruleSet;
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

	public static RuleSet logicalRuleSet() {
		return suiteCompileUtil.logicalRuleSet();
	}

	public static RuleSet funRuleSet() {
		return suiteCompileUtil.funRuleSet();
	}

	public static RuleSet eagerFunRuleSet() {
		return suiteCompileUtil.eagerFunRuleSet();
	}

	public static RuleSet lazyFunRuleSet() {
		return suiteCompileUtil.lazyFunRuleSet();
	}

	public static void precompile(String libraryName, ProverConfig proverConfig) {
		suiteCompileUtil.precompile(libraryName, proverConfig);
	}

	// --------------------------------
	// Evaluation utilities

	public static boolean proveThis(RuleSet rs, String gs) {
		return suiteEvaluationUtil.proveThis(rs, gs);
	}

	public static boolean evaluateLogical(String lps) {
		return evaluateLogical(Suite.parse(lps));
	}

	public static boolean evaluateLogical(Node lp) {
		return suiteEvaluationUtil.evaluateLogical(lp);
	}

	public static List<Node> evaluateLogical(Node lp, ProverConfig pc, boolean isDumpCode) {
		return suiteEvaluationUtil.evaluateLogical(lp, pc, isDumpCode);
	}

	public static Node evaluateEagerFun(String fp) {
		return evaluateFun(fp, false);
	}

	public static Node evaluateLazyFun(String fp) {
		return evaluateFun(fp, true);
	}

	public static Node evaluateFun(String fp, boolean isLazy) {
		return evaluateFun(Suite.fcc(fp, isLazy));
	}

	public static Node evaluateFun(FunCompilerConfig fcc) {
		return suiteEvaluationUtil.evaluateFun(fcc);
	}

	public static Node evaluateFunType(String fps) {
		return evaluateFunType(Suite.fcc(Suite.parse(fps)));
	}

	public static Node evaluateFunType(FunCompilerConfig fcc) {
		return suiteEvaluationUtil.evaluateFunType(fcc);
	}

	// --------------------------------
	// Import utilities

	public static boolean importFrom(RuleSet rs, String name) throws IOException {
		return suiteImportUtil.importFrom(rs, name);
	}

	public static boolean importFile(RuleSet rs, String filename) throws IOException {
		return suiteImportUtil.importFile(rs, filename);
	}

	public static boolean importResource(RuleSet rs, String classpath) throws IOException {
		return suiteImportUtil.importResource(rs, classpath);
	}

	public static Prover createProver(List<String> toImports) {
		return suiteImportUtil.createProver(toImports);
	}

	public static RuleSet createRuleSet(List<String> toImports) {
		return suiteImportUtil.createRuleSet(toImports);
	}

	// --------------------------------
	// Parse utilities

	public static Node parse(String s) {
		return suiteParseUtil.parse(s);
	}

	public static Node parse(InputStream is) throws IOException {
		return suiteParseUtil.parse(is);
	}

}
