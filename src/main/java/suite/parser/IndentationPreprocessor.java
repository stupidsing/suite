package suite.parser;

import java.util.ArrayList;
import java.util.List;

import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.text.Segment;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Turns indent patterns into parentheses, to provide Python-like parsing.
 *
 * @author ywsing
 */
public class IndentationPreprocessor implements Fun<String, String> {

	private Operator operators[];

	private class Run {
		private Segment segment;
		private String text;

		private Run(int start, int end) {
			this.segment = new Segment(start, end);
		}

		private Run(String text) {
			this.text = text;
		}
	}

	public IndentationPreprocessor(Operator[] operators) {
		this.operators = operators;
	}

	@Override
	public String apply(String in) {
		List<Run> parts = new ArrayList<>();
		int nLastIndents = 0;
		String lastIndent = "";
		int pos = 0;

		while (pos < in.length()) {
			int pos0 = pos;
			char ch;
			while (pos0 < in.length() && (ch = in.charAt(pos0)) != '\n' && Character.isWhitespace(ch))
				pos0++;

			int pos1 = ParseUtil.searchPosition(in, pos0, "\n", Assoc.RIGHT, false);
			int pos2 = Math.min(pos1 + 1, in.length()); // Includes line-feed

			String indent = in.substring(pos, pos0);
			String line = in.substring(pos0, pos1);
			int nIndents = pos0 - pos, length = pos1 - pos0;

			if (!lastIndent.startsWith(indent) && !indent.startsWith(lastIndent))
				throw new RuntimeException("Indent mismatch");

			if (length != 0) { // Ignore empty lines
				int startPos = 0, endPos = length;
				lastIndent = indent;

				// Find operators at beginning and end of line
				for (Operator operator : operators) {
					String name = operator.getName().trim();

					if (!name.isEmpty()) {
						if (line.startsWith(name + " "))
							startPos = Math.max(startPos, name.length() + 1);
						if (Util.stringEquals(line, name))
							startPos = Math.max(startPos, name.length());
						if (line.endsWith(name))
							endPos = Math.min(endPos, length - name.length());
					}
				}

				if (startPos > endPos) // When a line has only one operator
					startPos = 0;

				// Insert parentheses by line indentation
				while (nLastIndents > nIndents) {
					parts.add(new Run(") "));
					nLastIndents--;
				}
				parts.add(new Run(pos0, pos0 + startPos));
				while (nLastIndents < nIndents) {
					parts.add(new Run(" ("));
					nLastIndents++;
				}
				parts.add(new Run(pos0 + startPos, pos2));

				nLastIndents = nIndents;
			}

			pos = pos2;
		}

		while (nLastIndents-- > 0)
			parts.add(new Run(") "));

		return combineRuns(in, parts);
	}

	private String combineRuns(String in, List<Run> parts) {
		StringBuilder sb = new StringBuilder();
		for (Run part : parts)
			if (part.segment != null)
				sb.append(in.substring(part.segment.start, part.segment.end));
			else
				sb.append(part.text);
		return sb.toString();
	}

}
