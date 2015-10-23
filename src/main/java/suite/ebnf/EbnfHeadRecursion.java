package suite.ebnf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
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
		EbnfGrammar en1 = expand(en0);

		if (en1.type == EbnfGrammarType.NAMED_) {
			EbnfGrammar en2 = en1.children.get(0);
			String entity = en1.content;
			HeadRecursionForm hrf = getHeadRecursionForm(en2, entity);
			EbnfGrammar en3;

			if (!hrf.listc.isEmpty()) {
				EbnfGrammar enb = new EbnfGrammar(EbnfGrammarType.OR____, hrf.listb);
				EbnfGrammar enc = new EbnfGrammar(EbnfGrammarType.OR____, hrf.listc);
				en3 = new EbnfGrammar(EbnfGrammarType.REPT0H, entity, Arrays.asList(enb, enc));
			} else
				en3 = en1;

			return en3;
		} else
			return en0;
	}

	private HeadRecursionForm getHeadRecursionForm(EbnfGrammar en0, String entity) {
		List<EbnfGrammar> empty = Collections.emptyList();
		EbnfGrammar en = expand(en0);
		HeadRecursionForm hrf;

		if (en.type == EbnfGrammarType.AND___) {
			HeadRecursionForm hrf0 = getHeadRecursionForm(en.children.get(0), entity);
			List<EbnfGrammar> tail = Util.right(en.children, 1);

			Fun<List<EbnfGrammar>, List<EbnfGrammar>> fun = list -> Read.from(list).map(en_ -> {
				List<EbnfGrammar> ens1 = new ArrayList<>();
				ens1.add(en_);
				ens1.addAll(tail);
				return new EbnfGrammar(EbnfGrammarType.AND___, ens1);
			}).toList();

			hrf = new HeadRecursionForm(fun.apply(hrf0.listb), fun.apply(hrf0.listc));
		} else if (en.type == EbnfGrammarType.NAMED_ && Util.stringEquals(en.content, entity))
			hrf = new HeadRecursionForm(empty, Arrays.asList(new EbnfGrammar(EbnfGrammarType.NIL___)));
		else if (en.type == EbnfGrammarType.OR____) {
			List<EbnfGrammar> listb = new ArrayList<>();
			List<EbnfGrammar> listc = new ArrayList<>();

			Read.from(en.children).map(en_ -> getHeadRecursionForm(en_, entity)).sink(hrf1 -> {
				listb.addAll(hrf1.listb);
				listc.addAll(hrf1.listc);
			});

			hrf = new HeadRecursionForm(listb, listc);
		} else
			hrf = new HeadRecursionForm(Arrays.asList(en), empty);

		return hrf;
	}

	private EbnfGrammar expand(EbnfGrammar en) {
		if (en.type == EbnfGrammarType.ENTITY) {
			EbnfGrammar en1 = nodesByEntity.get(en.content);
			return en1 != null ? expand(en1) : en;
		} else
			return en;
	}

}
