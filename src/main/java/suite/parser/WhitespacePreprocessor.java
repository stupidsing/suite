package suite.parser;

import suite.text.Preprocess.Run;
import suite.util.SmartSplit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Unify all whitespaces to the space bar space (ASCII code 32).
 *
 * @author ywsing
 */
public class WhitespacePreprocessor {

	private SmartSplit ss = new SmartSplit();

	private Set<Character> whitespaces;

	public WhitespacePreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	public List<Run> preprocess(String in) {
		var runs = new ArrayList<Run>();
		var length = in.length();
		int pos0 = 0, pos = 0;
		var quote = 0;
		var backquote = false;

		while (pos < length) {
			var ch = in.charAt(pos++);

			if (ch != '`') {
				if ((quote = ss.getQuoteChange(quote, ch)) == 0 && whitespaces.contains(ch)) {
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
