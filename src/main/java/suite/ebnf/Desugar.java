package suite.ebnf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.ebnf.Grammar.GrammarType;

public class Desugar {

	private Map<String, Grammar> grammarByEntity = new HashMap<>();
	private Grammar nil = new Grammar(GrammarType.AND___);
	private int counter;

	public Desugar(Map<String, Grammar> grammarByEntity0) {
		for (Entry<String, Grammar> e : grammarByEntity0.entrySet())
			grammarByEntity.put(e.getKey(), desugar(e.getValue()));
	}

	public Map<String, Grammar> getGrammarByEntity() {
		return grammarByEntity;
	}

	private Grammar desugar(Grammar eg0) {
		Grammar egx;

		switch (eg0.type) {
		case OPTION:
			egx = new Grammar(GrammarType.OR____, Arrays.asList(eg0.children.get(0), nil));
			break;
		case OR____:
			egx = eg0;
			break;
		case REPT0_:
			egx = repeat(eg0.children.get(0));
			break;
		case REPT0H:
			Grammar bs = eg0.children.get(0);
			Grammar cs = eg0.children.get(1);
			if (Boolean.TRUE)
				egx = new Grammar(GrammarType.AND___, Arrays.asList(bs, repeat(cs)));
			else {
				String name = "$" + counter++;
				Grammar ege = new Grammar(GrammarType.ENTITY, name);
				egx = new Grammar(GrammarType.NAMED_, name //
						, new Grammar(GrammarType.OR____, //
								Arrays.asList(bs, new Grammar(GrammarType.AND___, Arrays.asList(ege, cs)))));
				grammarByEntity.put(name, egx);
			}
			break;
		case REPT1_:
			Grammar child = eg0.children.get(0);
			egx = new Grammar(GrammarType.AND___, Arrays.asList(child, repeat(child)));
			break;
		default:
			egx = eg0;
		}

		return egx;
	}

	private Grammar repeat(Grammar child) {
		String name = "$" + counter++;
		Grammar ege = new Grammar(GrammarType.ENTITY, name);
		Grammar ega = new Grammar(GrammarType.AND___, Arrays.asList(child, ege));
		Grammar ego = new Grammar(GrammarType.OR____, Arrays.asList(nil, ega));
		Grammar egn = new Grammar(GrammarType.NAMED_, name, ego);
		grammarByEntity.put(name, egn);
		return egn;
	}

}
