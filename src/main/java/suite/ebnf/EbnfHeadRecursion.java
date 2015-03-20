package suite.ebnf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.util.Util;

public class EbnfHeadRecursion {

	private Map<String, EbnfGrammar> nodesByEntity;

	public EbnfHeadRecursion(Map<String, EbnfGrammar> nodesByEntity) {
		this.nodesByEntity = nodesByEntity;
	}

	/**
	 * Transform head-recursion rule as follows:
	 *
	 * A = B0 | B1 | ... | Bm | A C0 | A C1 | ... | A Cn
	 *
	 * becomes
	 *
	 * A = (B0 | B1 | ... | Bm) (C0 | C1 | ... | Cn)*
	 */
	public EbnfGrammar reduceHeadRecursion(EbnfGrammar en0) {
		EbnfGrammar en = lookup(en0);
		EbnfGrammar en1;

		if (en.type == EbnfGrammarType.NAMED_)
			return new EbnfGrammar(EbnfGrammarType.NAMED_, en.content, reduceHeadRecursion(en.children.get(0)));
		else if (en.type == EbnfGrammarType.OR____) {
			List<EbnfGrammar> listb = new ArrayList<>();
			List<EbnfGrammar> listc = new ArrayList<>();

			for (EbnfGrammar childEn : en.children) {
				if (childEn.type == EbnfGrammarType.AND___) {
					List<EbnfGrammar> ens = childEn.children;

					if (lookup(ens.get(0)) == en) {
						listc.add(new EbnfGrammar(EbnfGrammarType.AND___, Util.right(ens, 1)));
						continue;
					}
				}

				listb.add(childEn);
			}

			if (!listc.isEmpty()) {
				EbnfGrammar enb = new EbnfGrammar(EbnfGrammarType.OR____, listb);
				EbnfGrammar enc = new EbnfGrammar(EbnfGrammarType.OR____, listc);
				en1 = new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(enb, new EbnfGrammar(EbnfGrammarType.REPT0_, enc)));
			} else
				en1 = en;
		} else
			en1 = en;

		return en1;
	}

	private EbnfGrammar lookup(EbnfGrammar en) {
		if (en.type == EbnfGrammarType.ENTITY) {
			EbnfGrammar en1 = nodesByEntity.get(en.content);
			return en1 != null ? lookup(en1) : en;
		} else if (en.type == EbnfGrammarType.NAMED_)
			return lookup(en.children.get(0));
		else
			return en;
	}

}
