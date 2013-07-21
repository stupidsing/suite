package suite.lp;

import java.io.IOException;
import java.io.InputStream;

import suite.lp.doer.TermParser;
import suite.lp.node.Node;

public class ParseUtil {

	private TermParser parser = new TermParser();

	public Node parse(String s) {
		return parser.parse(s);
	}

	public Node parse(InputStream is) throws IOException {
		return parser.parse(is);
	}

}
