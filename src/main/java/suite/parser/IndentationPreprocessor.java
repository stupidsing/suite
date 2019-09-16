package suite.parser;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.List;

import primal.Verbs.Equals;
import primal.Verbs.Is;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.text.Preprocess.Run;
import suite.text.Segment;
import suite.util.SmartSplit;

/**
 * Turns indent patterns into parentheses, to provide Python-like parsing.
 *
 * @author ywsing
 */
public class IndentationPreprocessor {

	private SmartSplit ss = new SmartSplit();

	private Operator[] operators;

	public IndentationPreprocessor(Operator[] operators) {
		this.operators = operators;
	}

	public List<Run> preprocess(String in) {
		var runs = new ArrayList<Run>();
		var nLastIndents = 0;
		var lastIndent = "";
		var pos = 0;
		var length = in.length();

		while (pos < length) {
			var pos0 = pos;
			char ch;
			while (pos0 < length && (ch = in.charAt(pos0)) != '\n' && Is.whitespace(ch))
				pos0++;

			var segment = ss.searchSegment(in.toCharArray(), Segment.of(pos0, length), "\n", Assoc.RIGHT, false);
			var pos1 = segment != null ? segment.start : length;
			var pos2 = segment != null ? segment.end : length; // includes LF

			var indent = in.substring(pos, pos0);
			var line = in.substring(pos0, pos1);
			var nIndents = pos0 - pos;
			int lineLength = pos1 - pos0;

			if (!lastIndent.startsWith(indent) && !indent.startsWith(lastIndent))
				fail("indent mismatch");

			if (lineLength != 0) { // ignore empty lines
				int startPos = 0, endPos = lineLength;
				lastIndent = indent;

				// find operators at beginning and end of line
				for (var operator : operators) {
					var name = operator.name_().trim();

					if (!name.isEmpty()) {
						if (line.startsWith(name + " "))
							startPos = max(startPos, name.length() + 1);
						if (Equals.string(line, name))
							startPos = max(startPos, name.length());
						if (line.endsWith(name))
							endPos = min(endPos, lineLength - name.length());
					}
				}

				if (endPos < startPos) // when a line has only one operator
					startPos = 0;

				// insert parentheses by line indentation
				while (nIndents < nLastIndents) {
					runs.add(new Run(") "));
					nLastIndents--;
				}
				runs.add(new Run(pos0, pos0 + startPos));
				while (nLastIndents < nIndents) {
					runs.add(new Run(" ("));
					nLastIndents++;
				}
				runs.add(new Run(pos0 + startPos, pos2));

				nLastIndents = nIndents;
			}

			pos = pos2;
		}

		while (0 < nLastIndents--)
			runs.add(new Run(") "));
		return runs;
	}

}
