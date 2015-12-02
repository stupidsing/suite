package suite.ebnf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.ebnf.EbnfGrammar.EbnfGrammarType;

public class EbnfDesugar {

	private Map<String, EbnfGrammar> grammarByEntity = new HashMap<>();
	private EbnfGrammar nil = new EbnfGrammar(EbnfGrammarType.AND___);
	private int counter;

	public EbnfDesugar(Map<String, EbnfGrammar> grammarByEntity0) {
		for (Entry<String, EbnfGrammar> entry : grammarByEntity0.entrySet())
			grammarByEntity.put(entry.getKey(), desugar(entry.getValue()));
	}

	public Map<String, EbnfGrammar> getGrammarByEntity() {
		return grammarByEntity;
	}

	private EbnfGrammar desugar(EbnfGrammar eg0) {
		EbnfGrammar egx;

		switch (eg0.type) {
		case OPTION:
			egx = new EbnfGrammar(EbnfGrammarType.OR____, Arrays.asList(eg0.children.get(0), nil));
			break;
		case OR____:
			egx = eg0;
			break;
		case REPT0_:
			egx = repeat(eg0.children.get(0));
			break;
		case REPT0H:
			EbnfGrammar bs = eg0.children.get(0);
			EbnfGrammar cs = eg0.children.get(1);
			if (Boolean.TRUE)
				egx = new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(bs, repeat(cs)));
			else {
				String name = "$" + counter++;
				EbnfGrammar ege = new EbnfGrammar(EbnfGrammarType.ENTITY, name);
				egx = new EbnfGrammar(EbnfGrammarType.NAMED_, name //
						, new EbnfGrammar(EbnfGrammarType.OR____, //
								Arrays.asList(bs, new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(ege, cs)))));
				grammarByEntity.put(name, egx);
			}
			break;
		case REPT1_:
			EbnfGrammar child = eg0.children.get(0);
			egx = new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(child, repeat(child)));
			break;
		default:
			egx = eg0;
		}

		return egx;
	}

	private EbnfGrammar repeat(EbnfGrammar child) {
		String name = "$" + counter++;
		EbnfGrammar ege = new EbnfGrammar(EbnfGrammarType.ENTITY, name);
		EbnfGrammar ega = new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(child, ege));
		EbnfGrammar ego = new EbnfGrammar(EbnfGrammarType.OR____, Arrays.asList(nil, ega));
		EbnfGrammar egn = new EbnfGrammar(EbnfGrammarType.NAMED_, name, ego);
		grammarByEntity.put(name, egn);
		return egn;
	}

}
