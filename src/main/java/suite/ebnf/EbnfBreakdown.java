package suite.ebnf;

import java.util.Arrays;
import java.util.List;

import suite.adt.Pair;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Read;
import suite.util.ParseUtil;
import suite.util.Util;

public class EbnfBreakdown {

	public EbnfGrammar breakdown(String name, String s) {
		return new EbnfGrammar(EbnfGrammarType.NAMED_, name, breakdown(s));
	}

	public EbnfGrammar breakdown(String s) {
		EbnfGrammar en;
		List<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if (1 < (list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size())
			en = new EbnfGrammar(EbnfGrammarType.OR____, breakdown(list));
		else if ((pair = ParseUtil.search(s, " /except/ ", Assoc.RIGHT)) != null)
			en = new EbnfGrammar(EbnfGrammarType.EXCEPT, Arrays.asList(breakdown(pair.t0), breakdown(pair.t1)));
		else if (1 < (list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size())
			en = new EbnfGrammar(EbnfGrammarType.AND___, breakdown(list));
		else if (s.equals(""))
			en = new EbnfGrammar(EbnfGrammarType.AND___);
		else if (s.endsWith("?"))
			en = new EbnfGrammar(EbnfGrammarType.OPTION, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("*"))
			en = new EbnfGrammar(EbnfGrammarType.REPT0_, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("+"))
			en = new EbnfGrammar(EbnfGrammarType.REPT1_, breakdown(Util.substr(s, 0, -1)));
		else if (s.startsWith("\"") && s.endsWith("\""))
			en = new EbnfGrammar(EbnfGrammarType.STRING, Escaper.unescape(Util.substr(s, 1, -1), "\""));
		else if (s.startsWith("(") && s.endsWith(")"))
			en = breakdown(Util.substr(s, 1, -1));
		else
			en = new EbnfGrammar(EbnfGrammarType.ENTITY, s);

		return en;
	}

	private List<EbnfGrammar> breakdown(List<String> list) {
		return Read.from(list).map(this::breakdown).toList();
	}

}
