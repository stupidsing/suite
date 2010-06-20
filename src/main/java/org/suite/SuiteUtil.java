package org.suite;

import java.io.IOException;
import java.io.InputStream;

import org.suite.doer.Generalizer;
import org.suite.doer.Parser;
import org.suite.doer.Prover;
import org.suite.kb.RuleSet;
import org.suite.node.Node;

public class SuiteUtil {

	private static Parser parser = new Parser();

	public static boolean proveThis(RuleSet rs, String s) {
		Node node = parse(s);
		node = new Generalizer().generalize(node);
		Prover prover = new Prover(rs);
		return prover.prove(node);
	}

	public static Node parse(String s) {
		return parser.parse(s);
	}

	public static Node parse(InputStream s) throws IOException {
		return parser.parse(s);
	}

}
