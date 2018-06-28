package suite.ebnf.topdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.ebnf.Grammar;
import suite.ebnf.Grammar.GrammarType;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;
import suite.util.List_;
import suite.util.String_;

/**
 * Transform head-recursion rule as follows:
 *
 * A = B0 | B1 | ... | Bm | A C0 | A C1 | ... | A Cn
 *
 * becomes
 *
 * A = (B0 | B1 | ... | Bm) (C0 | C1 | ... | Cn)*
 */
public class ReduceHeadRecursion {

	private Map<String, Grammar> grammarByEntity;

	private class HeadRecursionForm {
		private List<Grammar> listb;
		private List<Grammar> listc;

		private HeadRecursionForm(List<Grammar> listb, List<Grammar> listc) {
			this.listb = listb;
			this.listc = listc;
		}
	}

	public ReduceHeadRecursion(Map<String, Grammar> grammarByEntity) {
		this.grammarByEntity = grammarByEntity;
	}

	public Grammar reduce(Grammar en0) {
		var en1 = expand(en0);

		if (en1.type == GrammarType.NAMED_) {
			var en2 = en1.children.get(0);
			var entity = en1.content;
			var hrf = getHeadRecursionForm(en2, entity);
			Grammar en3;

			if (!hrf.listc.isEmpty()) {
				var enb = new Grammar(GrammarType.OR____, hrf.listb);
				var enc = new Grammar(GrammarType.OR____, hrf.listc);
				en3 = new Grammar(GrammarType.REPT0H, entity, List.of(enb, enc));
			} else
				en3 = en1;

			return en3;
		} else
			return en0;
	}

	private HeadRecursionForm getHeadRecursionForm(Grammar en0, String entity) {
		List<Grammar> empty = List.of();
		var en = expand(en0);
		HeadRecursionForm hrf;

		if (en.type == GrammarType.AND___ && en.children.isEmpty())
			hrf = new HeadRecursionForm(empty, empty);
		else if (en.type == GrammarType.AND___) {
			var hrf0 = getHeadRecursionForm(en.children.get(0), entity);
			var tail = List_.right(en.children, 1);

			Iterate<List<Grammar>> fun = list -> Read.from(list).map(en_ -> {
				var ens1 = new ArrayList<Grammar>();
				ens1.add(en_);
				ens1.addAll(tail);
				return new Grammar(GrammarType.AND___, ens1);
			}).toList();

			hrf = new HeadRecursionForm(fun.apply(hrf0.listb), fun.apply(hrf0.listc));
		} else if (en.type == GrammarType.NAMED_ && String_.equals(en.content, entity))
			hrf = new HeadRecursionForm(empty, List.of(new Grammar(GrammarType.AND___)));
		else if (en.type == GrammarType.OR____) {
			var hrfs = Read.from(en.children).map(en_ -> getHeadRecursionForm(en_, entity)).toList();
			var listb = Read.from(hrfs).flatMap(hrf_ -> hrf_.listb).toList();
			var listc = Read.from(hrfs).flatMap(hrf_ -> hrf_.listc).toList();
			hrf = new HeadRecursionForm(listb, listc);
		} else
			hrf = new HeadRecursionForm(List.of(en), empty);

		return hrf;
	}

	private Grammar expand(Grammar en) {
		if (en.type == GrammarType.ENTITY) {
			var en1 = grammarByEntity.get(en.content);
			return en1 != null ? expand(en1) : en;
		} else
			return en;
	}

}
