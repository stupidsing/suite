package suite.ebnf.lr;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.Verbs.Right;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.persistent.PerList;
import primal.streamlet.Streamlet2;
import suite.ebnf.Grammar;
import suite.ebnf.Grammar.GrammarType;

public class BuildLr {

	private int counter;
	private Map<String, Grammar> grammarByEntity;

	private ReadLookahead readLookahead;
	private Map<Pair<String, Set<String>>, Transition> transitions = new HashMap<>();
	private Set<Pair<Transition, Transition>> merges = new HashSet<>();

	public final State state0;
	public final Map<State, Transition> fsm = new HashMap<>();

	private record Blr(int nTokens, Transition next) {
	}

	public class Transition extends HashMap<String, Pair<State, Reduce>> {
		private static final long serialVersionUID = 1l;

		private Transition() {
		}

		public boolean equals(Object object) {
			return this == object;
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}

		private boolean putAll(Transition sourceMap) {
			var b = false;
			for (var e1 : sourceMap.entrySet())
				b |= put_(e1.getKey(), e1.getValue());
			return b;
		}

		// shift-reduce conflict ends in reduce
		private boolean put_(String key, Pair<State, Reduce> value1) {
			var value0 = get(key);
			var order0 = order(value0);
			var order1 = order(value1);
			if (order0 < order1) {
				put(key, value1);
				return true;
			} else if (order1 < order0 || Equals.ab(value0, value1))
				return false;
			else if (value0.k != null && value1.k != null) {

				// merge each children if both are shifts
				var transition0 = fsm.get(value0.k);
				var transition1 = fsm.get(value1.k);
				return merges.add(Pair.of(transition0, transition1));
			} else
				return fail("duplicate key " + key + " old (" + value0 + ") new (" + value1 + ")");
		}

		private int order(Pair<State, Reduce> pair) {
			if (pair == null) // nothing
				return 0;
			else if (pair.v != null) // reduce
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

	public BuildLr(Map<String, Grammar> grammarByEntity, String rootEntity) {
		this.grammarByEntity = grammarByEntity;
		readLookahead = new ReadLookahead(grammarByEntity);
		var nextx = kv("EOF", new State());
		state0 = newState(buildLrs(rootEntity, nextx.keySet()).next);
	}

	public Blr buildLrs(String entity, Set<String> follows) {
		var k = Pair.of(entity, follows);
		var keys0 = new HashSet<Pair<String, Set<String>>>();
		transitions.put(k, new Transition());

		while (keys0.size() < transitions.size()) {
			var keys1 = new HashSet<>(transitions.keySet());
			keys1.removeAll(keys0);

			for (var pair : keys1) {
				var next_ = transitions.get(pair);
				var nextx_ = newTransition(pair.v);

				var blr1 = build(pair.k, nextx_);
				merges.add(Pair.of(next_, blr1.next));
				keys0.add(pair);
			}
		}

		boolean b;
		do {
			b = false;
			for (var merge : new ArrayList<>(merges))
				b |= merge.k.putAll(merge.v);
		} while (b);

		return new Blr(1, transitions.get(k));
	}

	private Blr build(String entity, Transition nextx) {
		return build(PerList.end(), grammarByEntity.get(entity), nextx);
	}

	private Blr build(PerList<Pair<String, Set<String>>> ps, Grammar eg, Transition nextx) {
		Fun<Streamlet2<String, Transition>, Blr> mergeAll = st2 -> {
			var next = newTransition(readLookahead.readLookahead(eg, nextx.keySet()));
			var state1 = newState(nextx);
			st2.sink((egn, next1) -> {
				next.put_(egn, Pair.of(state1, null));
				merges.add(Pair.of(next, next1));
			});
			return new Blr(1, next);
		};

		return switch (eg.type) {
		case AND___ -> {
			if (!eg.children.isEmpty()) {
				var tail = new Grammar(GrammarType.AND___, Right.of(eg.children, 1));
				var blr1 = build(ps, tail, nextx);
				var blr0 = build(ps, eg.children.get(0), blr1.next);
				yield new Blr(blr0.nTokens + blr1.nTokens, blr0.next);
			} else
				yield new Blr(0, nextx);
		}
		case ENTITY -> {
			var k = Pair.of(eg.content, nextx.keySet());
			var next1 = transitions.computeIfAbsent(k, k_ -> new Transition());
			yield mergeAll.apply(Read.<String, Transition> empty2().cons(eg.content, next1));
		}
		case NAMED_ -> {
			var reduce = new Reduce();
			var next = newTransition(nextx.keySet(), Pair.of(null, reduce));
			var blr1 = build(ps, eg.children.get(0), next);
			reduce.n = blr1.nTokens;
			reduce.name = eg.content;
			yield new Blr(1, blr1.next);
		}
		case OR____ -> {
			var pairs = new ArrayList<Pair<String, Transition>>();
			for (var eg1 : Read.from(eg.children)) {
				var egn = "OR." + System.identityHashCode(eg1);
				pairs.add(Pair.of(egn, build(ps, new Grammar(GrammarType.NAMED_, egn, eg1), nextx).next));
			}
			yield mergeAll.apply(Read.from2(pairs));
		}
		case STRING -> {
			var state1 = newState(nextx);
			yield new Blr(1, kv(eg.content, state1));
		}
		default -> {
			yield fail("LR parser cannot recognize " + eg.type);
		}
		};
	}

	private State newState(Transition nextx) {
		var state = new State();
		fsm.put(state, nextx);
		return state;
	}

	private Transition newTransition(Set<String> keys) {
		Pair<State, Reduce> value = null;
		return newTransition(keys, value);
	}

	private Transition newTransition(Set<String> keys, Pair<State, Reduce> value) {
		var transition = new Transition();
		for (var key : keys)
			transition.put(key, value);
		return transition;
	}

	private Transition kv(String k, State v) {
		var transition = new Transition();
		transition.put(k, Pair.of(v, null));
		return transition;
	}

}
