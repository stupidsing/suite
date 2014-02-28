package suite.node.io;

import suite.node.io.Operator.Assoc;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Turns indent patterns into parentheses, to provide Python-like parsing.
 * 
 * @author ywsing
 */
public class IndentationProcessor implements Fun<String, String> {

	private Operator operators[];

	public IndentationProcessor(Operator[] operators) {
		this.operators = operators;
	}

	@Override
	public String apply(String in) {
		StringBuilder sb = new StringBuilder();
		int nLastIndents = 0;
		String lastIndent = "";

		while (!in.isEmpty()) {
			String line;
			String lr[];

			lr = ParseUtil.search(in, "\n", Assoc.RIGHT, false);
			if (lr != null) {
				line = lr[0];
				in = lr[1];
			} else {
				line = in;
				in = "";
			}

			int length = line.length(), nIndents = 0;
			while (nIndents < length && Character.isWhitespace(line.charAt(nIndents)))
				nIndents++;

			String indent = line.substring(0, nIndents);
			line = line.substring(nIndents).trim();

			if (!lastIndent.startsWith(indent) && !lastIndent.startsWith(lastIndent))
				throw new RuntimeException("Indent mismatch");

			lastIndent = indent;

			if ((length = line.length()) != 0) { // Ignore empty lines
				int startPos = 0, endPos = length;

				// Find operators at beginning and end of line
				for (Operator operator : operators) {
					String name = operator.getName().trim();

					if (!name.isEmpty()) {
						if (line.startsWith(name + " "))
							startPos = Math.max(startPos, 1 + name.length());
						if (Util.stringEquals(line, name))
							startPos = Math.max(startPos, name.length());
						if (line.endsWith(name))
							endPos = Math.min(endPos, length - name.length());
					}
				}

				if (startPos > endPos) // When a line has only one operator
					startPos = 0;

				// Insert parentheses by line indentation
				String decoratedLine = "";
				while (nLastIndents > nIndents) {
					decoratedLine += ") ";
					nLastIndents--;
				}
				decoratedLine += line.substring(0, startPos);
				while (nLastIndents < nIndents) {
					decoratedLine += " (";
					nLastIndents++;
				}
				decoratedLine += line.substring(startPos, endPos);
				decoratedLine += line.substring(endPos);

				sb.append(decoratedLine + "\n");
			}

			nLastIndents = nIndents;
		}

		while (nLastIndents-- > 0)
			sb.append(") ");

		return sb.toString();
	}

}
