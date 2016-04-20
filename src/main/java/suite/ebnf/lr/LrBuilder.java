package suite.ebnf.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.Pair;
import suite.ebnf.EbnfGrammar;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.immutable.IList;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class LrBuilder {

	private int counter;
	private Map<String, EbnfGrammar> grammarByEntity;

	private LookaheadReader lookaheadReader;
	private Map<Pair<String, Set<String>>, Transition> transitions = new HashMap<>();
	private Set<Pair<Transition, Transition>> merges = new HashSet<>();

	public final State state0;
	public final Map<State, Transition> fsm = new HashMap<>();

	private class BuildLr {
		private int nTokens;
		private final Transition next;

		private BuildLr(int nTokens, Transition next) {
			this.nTokens = nTokens;
			this.next = next;
		}
	}

	public class Transition extends HashMap<String, Pair<State, Reduce>> {
		private static final long serialVersionUID = 1l;

		private Transition() {
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}

		public boolean equals(Object object) {
			return this == object;
		}

		private boolean putAll(Transition sourceMap) {
			boolean b = false;
			for (Entry<String, Pair<State, Reduce>> e1 : sourceMap.entrySet())
				b |= put_(e1.getKey(), e1.getValue());
			return b;
		}

		// Shift-reduce conflict ends in reduce
		private boolean put_(String key, Pair<State, Reduce> value1) {
			Pair<State, Reduce> value0 = get(key);
			int order0 = order(value0);
			int order1 = order(value1);
			if (order0 < order1) {
				put(key, value1);
				return true;
			} else if (order1 < order0 || Objects.equals(value0, value1))
				return false;
			else if (value0.t0 != null && value1.t0 != null) {

				// Merge each children if both are shifts
				Transition transition0 = fsm.get(value0.t0);
				Transition transition1 = fsm.get(value1.t0);
				return merges.add(Pair.of(transition0, transition1));
			} else
				throw new RuntimeException("Duplicate key " + key + " old (" + value0 + ") new (" + value1 + ")");
		}

		private int order(Pair<State, Reduce> pair) {
			if (pair == null) // Nothing
				return 0;
			else if (pair.t1 != null) // Reduce
				return 1;
			else
				return 2;
		}
	}

	public class Reduce {
		private String name;
		private int n;

		private Reduce() {
		}

		public String name() {
			return name;
		}

		public int n() {
			return n;
		}

		public String toString() {
			return name + "/" + n;
		}
	}

	public class State {
		private int id = counter++;

		private State() {
		}

		public String toString() {
			return String.format("S%02d", id);
		}
	}

	public LrBuilder(Map<String, EbnfGrammar> grammarByEntity, String rootEntity) {
		this.grammarByEntity = grammarByEntity;
		lookaheadReader = new LookaheadReader(grammarByEntity);
		Transition nextx = kv("EOF", new State());
		state0 = newState(buildLrs(rootEntity, nextx.keySet()).next);
	}

	public BuildLr buildLrs(String entity, Set<String> follows) {
		Pair<String, Set<String>> k = Pair.of(entity, follows);
		Set<Pair<String, Set<String>>> keys0 = new HashSet<>();
		transitions.put(k, new Transition());

		while (keys0.size() < transitions.size()) {
			Set<Pair<String, Set<String>>> keys1 = new HashSet<>(transitions.keySet());
			keys1.removeAll(keys0);

			for (Pair<String, Set<String>> pair : keys1) {
				Transition next_ = transitions.get(pair);
				Transition nextx_ = newTransition(pair.t1);

				BuildLr buildLr1 = build(pair.t0, nextx_);
				merges.add(Pair.of(next_, buildLr1.next));
				keys0.add(pair);
			}
		}

		boolean b;
		do {
			b = false;
			for (Pair<Transition, Transition> merge : new ArrayList<>(merges))
				b |= merge.t0.putAll(merge.t1);
		} while (b);

		return new BuildLr(1, transitions.get(k));
	}

	private BuildLr build(String entity, Transition nextx) {
		return build(IList.end(), grammarByEntity.get(entity), nextx);
	}

	private BuildLr build(IList<Pair<String, Set<String>>> ps, EbnfGrammar eg, Transition nextx) {
		Fun<Streamlet2<String, Transition>, BuildLr> mergeAll = st2 -> {
			Transition next = newTransition(lookaheadReader.readLookaheadSet(eg, nextx.keySet()));
			State state1 = newState(nextx);
			st2.sink((egn, next1) -> {
				next.put_(egn, Pair.of(state1, null));
				merges.add(Pair.of(next, next1));
			});
			return new BuildLr(1, next);
		};

		Pair<String, Set<String>> k;
		BuildLr buildLr;

		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				EbnfGrammar tail = new EbnfGrammar(EbnfGrammarType.AND___, Util.right(eg.children, 1));
				BuildLr buildLr1 = build(ps, tail, nextx);
				BuildLr buildLr0 = build(ps, eg.children.get(0), buildLr1.next);
				buildLr = new BuildLr(buildLr0.nTokens + buildLr1.nTokens, buildLr0.next);
			} else
				buildLr = new BuildLr(0, nextx);
			break;
		case ENTITY:
			k = Pair.of(eg.content, nextx.keySet());
			Transition next1 = transitions.computeIfAbsent(k, k_ -> new Transition());
			buildLr = mergeAll.apply(Read.from2(eg.content, next1));
			break;
		case NAMED_:
			Reduce reduce = new Reduce();
			Transition next = newTransition(nextx.keySet(), Pair.of(null, reduce));
			BuildLr buildLr1 = build(ps, eg.children.get(0), next);
			reduce.n = buildLr1.nTokens;
			reduce.name = eg.content;
			buildLr = new BuildLr(1, buildLr1.next);
			break;
		case OR____:
			List<Pair<String, Transition>> pairs = new ArrayList<>();
			for (EbnfGrammar eg1 : Read.from(eg.children)) {
				String egn = "OR." + System.identityHashCode(eg1);
				pairs.add(Pair.of(egn, build(ps, new EbnfGrammar(EbnfGrammarType.NAMED_, egn, eg1), nextx).next));
			}
			buildLr = mergeAll.apply(Read.from2(pairs));
			break;
		case STRING:
			State state1 = newState(nextx);
			buildLr = new BuildLr(1, kv(eg.content, state1));
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return buildLr;
	}

	private State newState(Transition nextx) {
		State state = new State();
		fsm.put(state, nextx);
		return state;
	}

	private Transition newTransition(Set<String> keys) {
		Pair<State, Reduce> value = null;
		return newTransition(keys, value);
	}

	private Transition newTransition(Set<String> keys, Pair<State, Reduce> value) {
		Transition transition = new Transition();
		for (String key : keys)
			transition.put(key, value);
		return transition;
	}

	private Transition kv(String k, State v) {
		Transition transition = new Transition();
		transition.put(k, Pair.of(v, null));
		return transition;
	}

}
