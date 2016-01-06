package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private int counter;
	private String rootEntity;
	private Map<String, EbnfGrammar> grammarByEntity;

	private Map<Pair<String, Set<String>>, Transition> transitions = new HashMap<>();
	private List<Source<Boolean>> merges = new ArrayList<>();

	private State state0;
	private Map<State, Transition> fsm = new HashMap<>();

	private class LookaheadSet {
		private boolean isPassThru;
		private Set<String> lookaheads;

		private LookaheadSet(boolean isPassThru, Set<String> lookaheads) {
			this.isPassThru = isPassThru;
			this.lookaheads = lookaheads;
		}
	}

	private class BuildLr {
		private int nTokens;
		private Transition next;

		private BuildLr(int nTokens, Transition next) {
			this.nTokens = nTokens;
			this.next = next;
		}
	}

	private class Transition extends HashMap<String, Pair<State, Reduce>> {
		private static final long serialVersionUID = 1l;
	}

	private class Reduce {
		private String name;
		private int n;

		public String toString() {
			return name + "/" + n;
		}
	}

	private class State {
		private int id = counter++;

		public String toString() {
			return String.format("S%02d", id);
		}
	}

	public static EbnfLrParse of(String grammar, String rootEntity) {
		try (StringReader reader = new StringReader(grammar)) {
			return new EbnfLrParse(EbnfGrammar.parse(reader), rootEntity);
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity, String rootEntity) {
		this.grammarByEntity = grammarByEntity;
		this.rootEntity = rootEntity;
		state0 = newState(buildLrs(rootEntity, kv("EOF", new State())).next);
	}

	private BuildLr buildLrs(String entity, Transition nextx) {
		Set<Pair<String, Set<String>>> keys0 = new HashSet<>();
		BuildLr buildLr = buildLr(entity, nextx);

		while (keys0.size() < transitions.size()) {
			Set<Pair<String, Set<String>>> keys1 = new HashSet<>(transitions.keySet());
			keys1.removeAll(keys0);

			for (Pair<String, Set<String>> pair : keys1) {
				Transition next_ = transitions.get(pair);
				Transition nextx_ = newTransition(pair.t1);

				BuildLr buildLr1 = buildLr(pair.t0, nextx_);
				merges.add(() -> resolveAll(next_, buildLr1.next));
				keys0.add(pair);
			}
		}

		boolean b;
		do {
			b = false;
			for (Source<Boolean> merge : merges)
				b |= merge.source();
		} while (b);

		return buildLr;
	}

	private BuildLr buildLr(String entity, Transition nextx) {
		return buildLr(IList.end(), grammarByEntity.get(entity), nextx);
	}

	private BuildLr buildLr(IList<Pair<String, Set<String>>> ps, EbnfGrammar eg, Transition nextx) {
		Fun<Streamlet2<String, Transition>, BuildLr> mergeAll = st2 -> {
			Transition next = newTransition(readLookaheadSet(eg, nextx));
			State state1 = newState(nextx);
			st2.sink((egn, next1) -> {
				merges.add(() -> resolve(next, egn, Pair.of(state1, null)));
				merges.add(() -> resolveAll(next, next1));
			});
			return new BuildLr(1, next);
		};

		BuildLr buildLr;

		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				EbnfGrammar tail = new EbnfGrammar(EbnfGrammarType.AND___, Util.right(eg.children, 1));
				BuildLr buildLr1 = buildLr(ps, tail, nextx);
				BuildLr buildLr0 = buildLr(ps, eg.children.get(0), buildLr1.next);
				buildLr = new BuildLr(buildLr0.nTokens + buildLr1.nTokens, buildLr0.next);
			} else
				buildLr = new BuildLr(0, nextx);
			break;
		case ENTITY:
			Pair<String, Set<String>> p = Pair.of(eg.content, nextx.keySet());
			Transition next1 = transitions.computeIfAbsent(p, p_ -> new Transition());
			buildLr = mergeAll.apply(Read.from2(eg.content, next1));
			break;
		case NAMED_:
			Reduce reduce = new Reduce();
			Transition next = newTransition(nextx.keySet(), Pair.of(null, reduce));
			BuildLr buildLr1 = buildLr(ps, eg.children.get(0), next);
			reduce.n = buildLr1.nTokens;
			reduce.name = eg.content;
			buildLr = new BuildLr(1, buildLr1.next);
			break;
		case OR____:
			List<Pair<String, Transition>> pairs = new ArrayList<>();
			for (EbnfGrammar eg1 : Read.from(eg.children)) {
				String egn = "OR" + counter++;
				pairs.add(Pair.of(egn, buildLr(ps, new EbnfGrammar(EbnfGrammarType.NAMED_, egn, eg1), nextx).next));
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

	private Transition newTransition(Set<String> lookaheads) {
		Pair<State, Reduce> value = null;
		return newTransition(lookaheads, value);
	}

	private Transition newTransition(Set<String> lookaheads, Pair<State, Reduce> value) {
		Transition transition = new Transition();
		for (String lookahead : lookaheads)
			transition.put(lookahead, value);
		return transition;
	}

	private Set<String> readLookaheadSet(EbnfGrammar eg, Transition nextx) {
		LookaheadSet ls = readLookaheadSet(IList.end(), eg);
		Set<String> lookaheadSet = new HashSet<>();
		if (ls.isPassThru)
			lookaheadSet.addAll(nextx.keySet());
		lookaheadSet.addAll(ls.lookaheads);
		return lookaheadSet;
	}

	private LookaheadSet readLookaheadSet(IList<EbnfGrammar> stack, EbnfGrammar eg) {
		LookaheadSet ls;

		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				EbnfGrammar tail = new EbnfGrammar(EbnfGrammarType.AND___, Util.right(eg.children, 1));
				LookaheadSet ls0 = readLookaheadSet(stack, eg.children.get(0));
				if (ls0.isPassThru) {
					LookaheadSet ls1 = readLookaheadSet(stack, tail);
					ls1.lookaheads.addAll(ls0.lookaheads);
					ls = ls1;
				} else
					ls = ls0;
			} else
				ls = new LookaheadSet(true, new HashSet<>());
			break;
		case ENTITY:
			EbnfGrammar eg_ = grammarByEntity.get(eg.content);
			ls = !stack.contains(eg_) ? readLookaheadSet(IList.cons(eg_, stack), eg_) : new LookaheadSet(false, new HashSet<>());
			ls.lookaheads.add(eg.content);
			break;
		case NAMED_:
			ls = readLookaheadSet(stack, eg.children.get(0));
			break;
		case OR____:
			ls = new LookaheadSet(false, new HashSet<>());
			for (EbnfGrammar eg1 : eg.children) {
				LookaheadSet pair1 = readLookaheadSet(stack, eg1);
				ls.isPassThru |= pair1.isPassThru;
				ls.lookaheads.addAll(pair1.lookaheads);
			}
			break;
		case STRING:
			ls = new LookaheadSet(false, new HashSet<>(Arrays.asList(eg.content)));
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return ls;
	}

	public Node check(String in) {
		return parse(in);
	}

	public Node parse(String in) {
		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();

		System.out.println("shifts/reduces = " + list(fsm));
		System.out.println("Initial state = " + state0);

		return parse(source, state0);
	}

	private Node parse(Source<Node> tokens, State state) {
		Deque<Pair<Node, State>> stack = new ArrayDeque<>();
		Node token = tokens.source();

		while (true) {
			String lookahead = token != null ? token.entity : "EOF";
			Pair<State, Reduce> sr = shift(stack, state, lookahead);

			if (sr.t0 != null) { // Shift
				stack.push(Pair.of(token, state));
				state = sr.t0;
				token = tokens.source();
			} else { // Reduce
				Reduce reduce = sr.t1;
				IList<Node> nodes = IList.end();

				for (int i = 0; i < reduce.n; i++) {
					Pair<Node, State> ns = stack.pop();
					nodes = IList.cons(ns.t0, nodes);
					state = ns.t1;
				}

				Node token1 = new Node(reduce.name, 0, 0, Read.from(nodes).toList());

				if (rootEntity.equals(reduce.name) && stack.size() == 0 && token == null)
					return token1;

				// Force shift after reduce
				stack.push(Pair.of(token1, state));
				state = shift(stack, state, token1.entity).t0;
			}
		}
	}

	private Pair<State, Reduce> shift(Deque<Pair<Node, State>> stack, State state, String lookahead) {
		System.out.print("(S=" + state + ", Lookahead=" + lookahead + ", Stack=" + stack.size() + ")");
		Pair<State, Reduce> sr = fsm.get(state).get(lookahead);
		System.out.println(" => " + sr);
		return sr;
	}

	private boolean resolveAll(Transition targetMap, Transition sourceMap) {
		boolean b = false;
		for (Entry<String, Pair<State, Reduce>> e1 : sourceMap.entrySet())
			b |= resolve(targetMap, e1.getKey(), e1.getValue());
		return b;
	}

	// Shift-reduce conflict ends in reduce
	private boolean resolve(Transition map, String key, Pair<State, Reduce> value1) {
		Pair<State, Reduce> value0 = map.get(key);
		if (value1 == null)
			return false;
		else if (value0 == null || isShiftReduceConflict(value0, value1)) {
			map.put(key, value1);
			return true;
		} else if (value0.equals(value1) || isShiftReduceConflict(value1, value0))
			return false;
		else
			throw new RuntimeException("Duplicate key " + key + " old (" + value0 + ") new (" + value1 + ")");
	}

	private boolean isShiftReduceConflict(Pair<State, Reduce> shift, Pair<State, Reduce> reduce) {
		return shift.t1 == null && reduce.t1 != null;
	}

	private Transition kv(String k, State v) {
		Transition transition = new Transition();
		transition.put(k, Pair.of(v, null));
		return transition;
	}

	private <K, V> String list(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		Read.from(map) //
				.mapKey(Object::toString) //
				.sortByKey(Util::compare) //
				.sink((k, v) -> sb.append(k + " = " + v + "\n"));
		sb.append("}\n");
		return sb.toString();
	}

}
