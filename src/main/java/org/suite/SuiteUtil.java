package org.suite;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.instructionexecutor.FunctionInstructionExecutor;
import org.instructionexecutor.LogicInstructionExecutor;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser;
import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.util.Util;

public class SuiteUtil {

	private static TermParser parser = new TermParser();

	public static void addRule(RuleSet rs, String rule) {
		rs.addRule(parser.parse(rule));
	}

	public static void importFile(RuleSet rs, String filename)
			throws IOException {
		FileInputStream is = null;

		try {
			is = new FileInputStream(filename);
			rs.importFrom(SuiteUtil.parse(is));
		} finally {
			Util.closeQuietly(is);
		}
	}

	public static void importResource(RuleSet rs, String classpath)
			throws IOException {
		ClassLoader cl = SuiteUtil.class.getClassLoader();
		InputStream is = null;

		try {
			is = cl.getResourceAsStream(classpath);
			rs.importFrom(SuiteUtil.parse(is));
		} finally {
			Util.closeQuietly(is);
		}
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
		RuleSet rs = new RuleSet();

		try {
			SuiteUtil.importResource(rs, "auto.sl");
			SuiteUtil.importResource(rs, "lc.sl");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		Node node = SuiteUtil.parse("compile-logic .program .code");
		// + ", pp-list .code"

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Node variable = generalizer.getVariable(Atom.create(".program"));
		Node ics = generalizer.getVariable(Atom.create(".code"));

		((Reference) variable).bound(program);
		if (new Prover(rs).prove(node)) {
			Node result = new LogicInstructionExecutor(rs, ics).execute();
			return result == Atom.create("true");
		} else
			throw new RuntimeException("Logic compilation error");
	}

	public static Node evaluateFunctional(String program) {
		return evaluateFunctional(parse(program));
	}

	public static Node evaluateFunctional(Node program) {
		RuleSet rs = new RuleSet();

		try {
			SuiteUtil.importResource(rs, "auto.sl");
			SuiteUtil.importResource(rs, "fc.sl");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		Node node = SuiteUtil.parse("compile-function .program .code");
		// + ", pp-list .code"

		Generalizer generalizer = new Generalizer();
		node = generalizer.generalize(node);
		Node variable = generalizer.getVariable(Atom.create(".program"));
		Node ics = generalizer.getVariable(Atom.create(".code"));

		((Reference) variable).bound(program);
		if (new Prover(rs).prove(node))
			return new FunctionInstructionExecutor(ics).execute();
		else
			throw new RuntimeException("Function compilation error");
	}

	public static Node parse(String s) {
		return parser.parse(s);
	}

	public static Node parse(InputStream s) throws IOException {
		return parser.parse(s);
	}

}
