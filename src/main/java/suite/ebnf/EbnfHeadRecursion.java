package suite.ebnf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import suite.ebnf.EbnfNode.EbnfType;
import suite.util.Util;

public class EbnfHeadRecursion {

	private Map<String, EbnfNode> nodesByEntity;

	public EbnfHeadRecursion(Map<String, EbnfNode> nodesByEntity) {
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
	public EbnfNode reduceHeadRecursion(EbnfNode en0) {
		EbnfNode en = lookup(en0);
		EbnfNode en1;

		if (en.type == EbnfType.NAMED_)
			return new EbnfNode(EbnfType.NAMED_, en.content, reduceHeadRecursion(en.children.get(0)));
		else if (en.type == EbnfType.OR____) {
			List<EbnfNode> listb = new ArrayList<>();
			List<EbnfNode> listc = new ArrayList<>();

			for (EbnfNode childEn : en.children) {
				if (childEn.type == EbnfType.AND___) {
					List<EbnfNode> ens = childEn.children;

					if (lookup(ens.get(0)) == en) {
						listc.add(new EbnfNode(EbnfType.AND___, Util.right(ens, 1)));
						continue;
					}
				}

				listb.add(childEn);
			}

			if (!listc.isEmpty()) {
				EbnfNode enb = new EbnfNode(EbnfType.OR____, listb);
				EbnfNode enc = new EbnfNode(EbnfType.OR____, listc);
				en1 = new EbnfNode(EbnfType.AND___, Arrays.asList(enb, new EbnfNode(EbnfType.REPT0_, enc)));
			} else
				en1 = en;
		} else
			en1 = en;

		return en1;
	}

	private EbnfNode lookup(EbnfNode en) {
		if (en.type == EbnfType.ENTITY) {
			EbnfNode en1 = nodesByEntity.get(en.content);
			return en1 != null ? lookup(en1) : en;
		} else if (en.type == EbnfType.NAMED_)
			return lookup(en.children.get(0));
		else
			return en;
	}

}
