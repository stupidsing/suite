package suite.parser;

import java.util.Set;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Unify all whitespaces to the space bar space (ASCII code 32).
 * 
 * @author ywsing
 */
public class WhitespacePreprocessor implements Fun<String, String> {

	private Set<Character> whitespaces;

	public WhitespacePreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		StringBuilder sb = new StringBuilder();
		int pos = 0;
		int quote = 0;
		boolean backquote = false;

		while (pos < in.length()) {
			char ch = in.charAt(pos++);

			if (ch != '`') {
				quote = ParseUtil.getQuoteChange(quote, ch);
				sb.append(quote == 0 && whitespaces.contains(ch) ? " " : ch);
			} else
				sb.append(quote == 0 ? (backquote = !backquote) ? "` " : " `" : ch);
		}

		return sb.toString();
	}

}
