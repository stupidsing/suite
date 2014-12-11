package suite.ebnf;

import java.util.Arrays;
import java.util.List;

import suite.ebnf.EbnfNode.EbnfType;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Read;
import suite.util.Pair;
import suite.util.ParseUtil;
import suite.util.Util;

public class EbnfBreakdown {

	public EbnfNode breakdown(String s) {
		EbnfNode en;
		List<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if ((list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size() > 1)
			en = new EbnfNode(EbnfType.OR____, breakdown(list));
		else if ((pair = ParseUtil.search(s, " /except/ ", Assoc.RIGHT)) != null)
			en = new EbnfNode(EbnfType.EXCEPT, Arrays.asList(breakdown(pair.t0), breakdown(pair.t1)));
		else if ((list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size() > 1)
			en = new EbnfNode(EbnfType.AND___, breakdown(list));
		else if (s.endsWith("*"))
			en = new EbnfNode(EbnfType.REPT0_, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("+"))
			en = new EbnfNode(EbnfType.REPT1_, breakdown(Util.substr(s, 0, -1)));
		else if (s.endsWith("?"))
			en = new EbnfNode(EbnfType.OPTION, breakdown(Util.substr(s, 0, -1)));
		else if (s.startsWith("(") && s.endsWith(")"))
			en = breakdown(Util.substr(s, 1, -1));
		else if (s.startsWith("\"") && s.endsWith("\""))
			en = new EbnfNode(EbnfType.STRING, Escaper.unescape(Util.substr(s, 1, -1), "\""));
		else
			en = new EbnfNode(EbnfType.ENTITY, s);

		return en;
	}

	private List<EbnfNode> breakdown(List<String> list) {
		return Read.from(list).map(this::breakdown).toList();
	}

}
