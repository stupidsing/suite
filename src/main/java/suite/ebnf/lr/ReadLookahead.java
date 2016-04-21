package suite.ebnf.lr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.ebnf.EbnfGrammar;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.util.Util;

public class ReadLookahead {

	private Map<String, EbnfGrammar> grammarByEntity;
	private Map<EbnfGrammar, LookaheadSet> lookaheadSets = new HashMap<>();

	public ReadLookahead(Map<String, EbnfGrammar> grammarByEntity) {
		super();
		this.grammarByEntity = grammarByEntity;
	}

	private class LookaheadSet {
		private boolean isPassThru = false;
		private Set<String> lookaheads = new HashSet<>();

		private void merge(LookaheadSet ls) {
			isPassThru |= ls.isPassThru;
			lookaheads.addAll(ls.lookaheads);
		}
	}

	public Set<String> readLookahead(EbnfGrammar eg, Set<String> follows) {
		LookaheadSet ls = readLookahead(eg);
		Set<String> lookaheadSet = new HashSet<>();
		if (ls.isPassThru)
			lookaheadSet.addAll(follows);
		lookaheadSet.addAll(ls.lookaheads);
		return lookaheadSet;
	}

	private LookaheadSet readLookahead(EbnfGrammar eg) {
		LookaheadSet ls = lookaheadSets.get(eg);
		if (ls == null) {
			lookaheadSets.put(eg, ls = new LookaheadSet());
			mergeLookahead(eg, ls);
		}
		return ls;
	}

	private void mergeLookahead(EbnfGrammar eg, LookaheadSet ls) {
		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				LookaheadSet ls0 = readLookahead(eg.children.get(0));
				ls.lookaheads.addAll(ls0.lookaheads);
				if (ls0.isPassThru) {
					EbnfGrammar tail = new EbnfGrammar(EbnfGrammarType.AND___, Util.right(eg.children, 1));
					ls.merge(readLookahead(tail));
				}
			}
			break;
		case ENTITY:
			ls.merge(readLookahead(grammarByEntity.get(eg.content)));
			break;
		case NAMED_:
			ls.merge(readLookahead(eg.children.get(0)));
			break;
		case OR____:
			for (EbnfGrammar eg1 : eg.children)
				ls.merge(readLookahead(eg1));
			break;
		case STRING:
			ls.lookaheads.add(eg.content);
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}
	}

}
