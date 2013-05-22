package org.suite;

import java.io.IOException;
import java.io.InputStream;

import org.suite.doer.TermParser;
import org.suite.node.Node;

public class SuiteParseUtil {

	private TermParser parser = new TermParser();

	public Node parse(String s) {
		return parser.parse(s);
	}

	public Node parse(InputStream is) throws IOException {
		return parser.parse(is);
	}

}
