package suite.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import suite.text.Preprocess.Run;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Unify all whitespaces to the space bar space (ASCII code 32).
 *
 * @author ywsing
 */
public class WhitespacePreprocessor implements Fun<String, List<Run>> {

	private Set<Character> whitespaces;

	public WhitespacePreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public List<Run> apply(String in) {
		List<Run> runs = new ArrayList<>();
		var length = in.length();
		int pos0 = 0, pos = 0;
		var quote = 0;
		boolean backquote = false;

		while (pos < length) {
			var ch = in.charAt(pos++);

			if (ch != '`') {
				if ((quote = ParseUtil.getQuoteChange(quote, ch)) == 0 && whitespaces.contains(ch)) {
					runs.add(new Run(pos0, pos - 1));
					runs.add(new Run(" "));
					pos0 = pos;
				}
			} else if (quote == 0) {
				var split = (backquote = !backquote) ? pos : pos - 1;
				runs.add(new Run(pos0, split));
				runs.add(new Run(" "));
				pos0 = split;
			}
		}

		runs.add(new Run(pos0, length));
		return runs;
	}

}
