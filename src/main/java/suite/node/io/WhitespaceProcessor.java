package suite.node.io;

import java.util.Set;

import suite.util.FunUtil.Fun;
import suite.util.Util;

/**
 * Unify all whitespaces to the space bar space (ASCII code 32).
 * 
 * @author ywsing
 */
public class WhitespaceProcessor implements Fun<String, String> {

	private Set<Character> whitespaces;

	public WhitespaceProcessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		StringBuilder sb = new StringBuilder();
		int count = 0;

		for (char ch : Util.chars(in))
			if (whitespaces.contains(ch))
				count++;
			else {
				if (count > 0) {
					sb.append(" ");
					count = 0;
				}

				sb.append(ch);
			}

		return sb.toString();
	}

}
