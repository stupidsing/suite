package suite;

import java.io.IOException;
import java.io.InputStream;

import suite.node.Node;
import suite.node.io.TermParser;

public class ParseUtil {

	private TermParser parser = new TermParser();

	public Node parse(String s) {
		return parser.parse(s);
	}

	public Node parse(InputStream is) throws IOException {
		return parser.parse(is);
	}

}
