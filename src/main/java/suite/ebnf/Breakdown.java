package suite.ebnf;

import java.util.List;

import suite.adt.pair.Pair;
import suite.ebnf.Grammar.GrammarType;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Read;
import suite.util.ParseUtil;
import suite.util.String_;

public class Breakdown {

	public Grammar breakdown(String name, String s) {
		return new Grammar(GrammarType.NAMED_, name, breakdown(s));
	}

	public Grammar breakdown(String s) {
		Grammar eg;
		List<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if (1 < (list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.OR____, breakdown(list));
		else if ((pair = ParseUtil.search(s, " /except/ ", Assoc.RIGHT)) != null)
			eg = new Grammar(GrammarType.EXCEPT, List.of(breakdown(pair.t0), breakdown(pair.t1)));
		else if (1 < (list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size())
			eg = new Grammar(GrammarType.AND___, breakdown(list));
		else if (s.equals(""))
			eg = new Grammar(GrammarType.AND___);
		else if (s.endsWith("!"))
			eg = new Grammar(GrammarType.ONCE__, breakdown(String_.range(s, 0, -1)));
		else if (s.endsWith("?"))
			eg = new Grammar(GrammarType.OPTION, breakdown(String_.range(s, 0, -1)));
		else if (s.endsWith("*"))
			eg = new Grammar(GrammarType.REPT0_, breakdown(String_.range(s, 0, -1)));
		else if (s.endsWith("+"))
			eg = new Grammar(GrammarType.REPT1_, breakdown(String_.range(s, 0, -1)));
		else if (s.startsWith("\"") && s.endsWith("\""))
			eg = new Grammar(GrammarType.STRING, Escaper.unescape(String_.range(s, 1, -1), "\""));
		else if (s.startsWith("(") && s.endsWith(")")) {
			var s1 = String_.range(s, 1, -1);
			eg = 0 < (pair = String_.split2(s1, "@")).t1.length() ? pair.map(this::breakdown) : breakdown(s1);
		} else
			eg = new Grammar(GrammarType.ENTITY, s);

		return eg;
	}

	private List<Grammar> breakdown(List<String> list) {
		return Read.from(list).map(this::breakdown).toList();
	}

}
