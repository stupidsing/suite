package suite.node.io;

import java.util.List;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Unify all whitespaces to the space bar space (ASCII code 32).
 * 
 * @author ywsing
 */
public class WhitespaceProcessor implements Fun<String, String> {

	private List<Character> whitespaces;

	public WhitespaceProcessor(List<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		for (char whitespace : whitespaces)
			in = replace(in, "" + whitespace, " ");
		return in;
	}

	private String replace(String in, String from, String to) {
		while (true) {
			int pos = ParseUtil.search(in, 0, from);

			if (pos != -1)
				in = in.substring(0, pos) + to + in.substring(pos + from.length());
			else
				return in;
		}
	}

}
