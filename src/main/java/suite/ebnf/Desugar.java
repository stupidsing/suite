package suite.ebnf;

import suite.ebnf.Grammar.GrammarType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Desugar {

	private Map<String, Grammar> grammarByEntity = new HashMap<>();
	private Grammar nil = new Grammar(GrammarType.AND___);
	private int counter;

	public Desugar(Map<String, Grammar> grammarByEntity0) {
		for (var e : grammarByEntity0.entrySet())
			grammarByEntity.put(e.getKey(), desugar(e.getValue()));
	}

	public Map<String, Grammar> getGrammarByEntity() {
		return grammarByEntity;
	}

	private Grammar desugar(Grammar eg0) {
		Grammar egx;

		switch (eg0.type) {
		case OPTION:
			egx = new Grammar(GrammarType.OR____, List.of(eg0.children.get(0), nil));
			break;
		case OR____:
			egx = eg0;
			break;
		case REPT0_:
			egx = repeat(eg0.children.get(0));
			break;
		case REPT0H:
			var bs = eg0.children.get(0);
			var cs = eg0.children.get(1);
			if (Boolean.TRUE)
				egx = new Grammar(GrammarType.AND___, List.of(bs, repeat(cs)));
			else {
				var name = "$" + counter++;
				var ege = new Grammar(GrammarType.ENTITY, name);
				egx = new Grammar(GrammarType.NAMED_, name
						, new Grammar(GrammarType.OR____,
								List.of(bs, new Grammar(GrammarType.AND___, List.of(ege, cs)))));
				grammarByEntity.put(name, egx);
			}
			break;
		case REPT1_:
			var child = eg0.children.get(0);
			egx = new Grammar(GrammarType.AND___, List.of(child, repeat(child)));
			break;
		default:
			egx = eg0;
		}

		return egx;
	}

	private Grammar repeat(Grammar child) {
		var name = "$" + counter++;
		var ege = new Grammar(GrammarType.ENTITY, name);
		var ega = new Grammar(GrammarType.AND___, List.of(child, ege));
		var ego = new Grammar(GrammarType.OR____, List.of(nil, ega));
		var egn = new Grammar(GrammarType.NAMED_, name, ego);
		grammarByEntity.put(name, egn);
		return egn;
	}

}
