package suite.ebnf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.streamlet.Read;
import suite.util.Util;

/**
 * Transform head-recursion rule as follows:
 *
 * A = B0 | B1 | ... | Bm | A C0 | A C1 | ... | A Cn
 *
 * becomes
 *
 * A = (B0 | B1 | ... | Bm) (C0 | C1 | ... | Cn)*
 */
public class EbnfHeadRecursion {

	private Map<String, EbnfGrammar> nodesByEntity;

	private class HeadRecursionForm {
		private List<EbnfGrammar> listb;
		private List<EbnfGrammar> listc;

		private HeadRecursionForm(List<EbnfGrammar> listb, List<EbnfGrammar> listc) {
			this.listb = listb;
			this.listc = listc;
		}
	}

	public EbnfHeadRecursion(Map<String, EbnfGrammar> nodesByEntity) {
		this.nodesByEntity = nodesByEntity;
	}

	public EbnfGrammar reduceHeadRecursion(EbnfGrammar en0) {
		EbnfGrammar en = expand(en0);
		if (en.type == EbnfGrammarType.NAMED_)
			return reduceHeadRecursion(en.children.get(0), en.content);
		else
			return en0;
	}

	public EbnfGrammar reduceHeadRecursion(EbnfGrammar en0, String entity) {
		HeadRecursionForm hrf = getHeadRecursionForm(en0, entity);
		EbnfGrammar en1;

		if (!hrf.listc.isEmpty()) {
			EbnfGrammar enb = new EbnfGrammar(EbnfGrammarType.OR____, hrf.listb);
			EbnfGrammar enc = new EbnfGrammar(EbnfGrammarType.OR____, hrf.listc);
			en1 = new EbnfGrammar(EbnfGrammarType.REPT0H, entity, Arrays.asList(enb, enc));
		} else
			en1 = new EbnfGrammar(EbnfGrammarType.NAMED_, entity, Arrays.asList(en0));

		return en1;
	}

	public HeadRecursionForm getHeadRecursionForm(EbnfGrammar en0, String entity) {
		EbnfGrammar en = expand(en0);
		List<EbnfGrammar> ens;
		HeadRecursionForm hrf;

		if (en.type == EbnfGrammarType.OR____) {
			List<EbnfGrammar> listb = new ArrayList<>();
			List<EbnfGrammar> listc = new ArrayList<>();

			for (HeadRecursionForm hrf1 : Read.from(en.children).map(en_ -> getHeadRecursionForm(en_, entity))) {
				listb.addAll(hrf1.listb);
				listc.addAll(hrf1.listc);
			}

			hrf = new HeadRecursionForm(listb, listc);
		} else if (en.type == EbnfGrammarType.AND___ && Util.stringEquals(name((ens = en.children).get(0)), entity))
			hrf = new HeadRecursionForm(Collections.emptyList(), Util.right(ens, 1));
		else
			hrf = new HeadRecursionForm(Arrays.asList(en), Collections.emptyList());

		return hrf;
	}

	private String name(EbnfGrammar en0) {
		EbnfGrammar en = expand(en0);
		return en.type == EbnfGrammarType.NAMED_ ? en.content : null;
	}

	private EbnfGrammar expand(EbnfGrammar en) {
		if (en.type == EbnfGrammarType.ENTITY) {
			EbnfGrammar en1 = nodesByEntity.get(en.content);
			return en1 != null ? expand(en1) : en;
		} else
			return en;
	}

}
