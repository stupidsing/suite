package suite.node.parser;

import primal.Verbs.Get;
import primal.Verbs.Is;
import primal.Verbs.Substring;
import primal.os.Log_;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.io.Escaper;
import suite.util.SmartSplit;

public class TerminalParser {

	private SmartSplit ss = new SmartSplit();

	public Node parseTerminal(String s) {
		if (!s.isEmpty()) {
			var first = Get.ch(s, 0);
			var last = Get.ch(s, -1);

			if (Is.integer(s))
				return Int.of(Integer.parseInt(s));
			if (s.startsWith("+x")) // allows +xFFFFFFFF
				return Int.of((int) Long.parseLong(s.substring(2), 16));
			if (s.startsWith("+'") && s.endsWith("'") && s.length() == 4)
				return Int.of(s.charAt(2));

			if (first == '"' && last == '"')
				return new Str(Escaper.unescape(Substring.of(s, 1, -1), "\""));

			if (first == '\'' && last == '\'')
				s = Escaper.unescape(Substring.of(s, 1, -1), "'");
			else {
				s = s.trim(); // trim unquoted atoms
				if (!ss.isParseable(s))
					Log_.info("Suspicious input when parsing " + s);
			}

			return Atom.of(s);
		} else
			return Atom.NIL;
	}

}
