package suite.node.parser;

import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.io.Escaper;
import suite.os.Log_;
import suite.util.ParseUtil;
import suite.util.String_;

public class TerminalParser {

	public Node parseTerminal(String s) {
		if (!s.isEmpty()) {
			var first = String_.charAt(s, 0);
			var last = String_.charAt(s, -1);

			if (String_.isInteger(s))
				return Int.of(Integer.parseInt(s));
			if (s.startsWith("+x")) // allows +xFFFFFFFF
				return Int.of((int) Long.parseLong(s.substring(2), 16));
			if (s.startsWith("+'") && s.endsWith("'") && s.length() == 4)
				return Int.of(s.charAt(2));

			if (first == '"' && last == '"')
				return new Str(Escaper.unescape(String_.range(s, 1, -1), "\""));

			if (first == '\'' && last == '\'')
				s = Escaper.unescape(String_.range(s, 1, -1), "'");
			else {
				s = s.trim(); // trim unquoted atoms
				if (!ParseUtil.isParseable(s))
					Log_.info("Suspicious input when parsing " + s);
			}

			return Atom.of(s);
		} else
			return Atom.NIL;
	}

}
