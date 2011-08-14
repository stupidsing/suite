package org.suite;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.util.Util;

public class SuiteUtil {

	private static TermParser parser = new TermParser();

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

	public static Node parse(String s) {
		return parser.parse(s);
	}

	public static Node parse(InputStream s) throws IOException {
		return parser.parse(s);
	}

}
