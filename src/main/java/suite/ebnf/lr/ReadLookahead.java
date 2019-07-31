package suite.ebnf.lr;

import static primal.statics.Fail.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.ebnf.Grammar;
import suite.ebnf.Grammar.GrammarType;
import suite.util.List_;

public class ReadLookahead {

	private Map<String, Grammar> grammarByEntity;
	private Map<Grammar, LookaheadSet> lookaheadSets = new HashMap<>();

	public ReadLookahead(Map<String, Grammar> grammarByEntity) {
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

	public Set<String> readLookahead(Grammar eg, Set<String> follows) {
		var ls = readLookahead(eg);
		var lookaheadSet = new HashSet<>(ls.lookaheads);
		if (ls.isPassThru)
			lookaheadSet.addAll(follows);
		return lookaheadSet;
	}

	private LookaheadSet readLookahead(Grammar eg) {
		var ls = lookaheadSets.get(eg);
		if (ls == null) {
			lookaheadSets.put(eg, ls = new LookaheadSet());
			mergeLookahead(eg, ls);
		}
		return ls;
	}

	private void mergeLookahead(Grammar eg, LookaheadSet ls) {
		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				var ls0 = readLookahead(eg.children.get(0));
				ls.lookaheads.addAll(ls0.lookaheads);
				if (ls0.isPassThru) {
					var tail = new Grammar(GrammarType.AND___, List_.right(eg.children, 1));
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
			for (var eg1 : eg.children)
				ls.merge(readLookahead(eg1));
			break;
		case STRING:
			ls.lookaheads.add(eg.content);
			break;
		default:
			fail("LR parser cannot recognize " + eg.type);
		}
	}

}
