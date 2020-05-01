package suite.ebnf;

import primal.Verbs.Substring;
import primal.adt.Pair;
import primal.parser.Operator.Assoc;
import primal.streamlet.Streamlet;
import suite.ebnf.Grammar.GrammarType;
import suite.node.io.Escaper;
import suite.util.SmartSplit;

import java.util.List;

public class Breakdown {

	private SmartSplit ss = new SmartSplit();

	public Grammar breakdown(String name, String s) {
		return new Grammar(GrammarType.NAMED_, name, breakdown(s));
	}

	public Grammar breakdown(String s) {
		Grammar eg;
		Streamlet<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if (1 < (list = ss.splitn(s, " | ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.OR____, breakdown(list));
		else if ((pair = ss.split(s, " /except/ ", Assoc.RIGHT)) != null)
			eg = new Grammar(GrammarType.EXCEPT, List.of(breakdown(pair.k), breakdown(pair.v)));
		else if (1 < (list = ss.splitn(s, " ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.AND___, breakdown(list));
		else if (s.equals(""))
			eg = new Grammar(GrammarType.AND___);
		else if (s.endsWith("!"))
			eg = new Grammar(GrammarType.ONCE__, breakdown(Substring.of(s, 0, -1)));
		else if (s.endsWith("?"))
			eg = new Grammar(GrammarType.OPTION, breakdown(Substring.of(s, 0, -1)));
		else if (s.endsWith("*"))
			eg = new Grammar(GrammarType.REPT0_, breakdown(Substring.of(s, 0, -1)));
		else if (s.endsWith("+"))
			eg = new Grammar(GrammarType.REPT1_, breakdown(Substring.of(s, 0, -1)));
		else if (s.startsWith("@\"") && s.endsWith("\"")) {
			var s1 = str(s.substring(1));
			eg = new Grammar(GrammarType.NAMED_, s1, new Grammar(GrammarType.STRING, s1));
		} else if (s.startsWith("\"") && s.endsWith("\""))
			eg = new Grammar(GrammarType.STRING, str(s));
		else if (s.startsWith("(") && s.endsWith(")"))
			eg = breakdown(Substring.of(s, 1, -1));
		else
			eg = new Grammar(GrammarType.ENTITY, s);

		return eg;
	}

	private List<Grammar> breakdown(Streamlet<String> list) {
		return list.map(this::breakdown).toList();
	}

	private String str(String s) {
		return Escaper.unescape(Substring.of(s, 1, -1), "\"");
	}

}
