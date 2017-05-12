package suite.parser;

import java.util.ArrayList;
import java.util.List;

import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.text.Preprocess.Run;
import suite.text.Segment;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.String_;

/**
 * Turns indent patterns into parentheses, to provide Python-like parsing.
 *
 * @author ywsing
 */
public class IndentationPreprocessor implements Fun<String, List<Run>> {

	private Operator[] operators;

	public IndentationPreprocessor(Operator[] operators) {
		this.operators = operators;
	}

	@Override
	public List<Run> apply(String in) {
		List<Run> runs = new ArrayList<>();
		int nLastIndents = 0;
		String lastIndent = "";
		int pos = 0;
		int length = in.length();

		while (pos < length) {
			int pos0 = pos;
			char ch;
			while (pos0 < length && (ch = in.charAt(pos0)) != '\n' && Character.isWhitespace(ch))
				pos0++;

			Segment segment = ParseUtil.searchPosition(in.toCharArray(), Segment.of(pos0, length), "\n", Assoc.RIGHT, false);
			int pos1 = segment != null ? segment.start : length;
			int pos2 = segment != null ? segment.end : length; // includes LF

			String indent = in.substring(pos, pos0);
			String line = in.substring(pos0, pos1);
			int nIndents = pos0 - pos, lineLength = pos1 - pos0;

			if (!lastIndent.startsWith(indent) && !indent.startsWith(lastIndent))
				throw new RuntimeException("Indent mismatch");

			if (lineLength != 0) { // ignore empty lines
				int startPos = 0, endPos = lineLength;
				lastIndent = indent;

				// find operators at beginning and end of line
				for (Operator operator : operators) {
					String name = operator.getName().trim();

					if (!name.isEmpty()) {
						if (line.startsWith(name + " "))
							startPos = Math.max(startPos, name.length() + 1);
						if (String_.equals(line, name))
							startPos = Math.max(startPos, name.length());
						if (line.endsWith(name))
							endPos = Math.min(endPos, lineLength - name.length());
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
