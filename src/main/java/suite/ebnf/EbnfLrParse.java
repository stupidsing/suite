package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private int counter;
	private String rootEntity;
	private Map<String, EbnfGrammar> grammarByEntity;

	private State state0;
	private Map<State, Map<String, Pair<State, Reduce>>> fsm = new HashMap<>();

	private class BuildLr {
		private int nTokens;
		private Map<String, Pair<State, Reduce>> next;

		private BuildLr(int nTokens, Map<String, Pair<State, Reduce>> next) {
			this.nTokens = nTokens;
			this.next = next;
		}
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

		EbnfGrammar eg = grammarByEntity.get(rootEntity);
		state0 = newState(buildLr(IList.end(), eg, kv("EOF", Pair.of(new State(), null))).next);
	}

	private BuildLr buildLr(IList<Pair<String, Set<String>>> ps, EbnfGrammar eg, Map<String, Pair<State, Reduce>> nextx) {
		Map<String, Pair<State, Reduce>> next;
		State state1;
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
			state1 = newState(nextx);
			next = kv(eg.content, Pair.of(state1, null));
			Pair<String, Set<String>> p = Pair.of(eg.content, nextx.keySet());
			if (!ps.contains(p))
				resolveAll(next, buildLr(IList.cons(p, ps), grammarByEntity.get(eg.content), nextx).next);
			buildLr = new BuildLr(1, next);
			break;
		case NAMED_:
			Reduce reduce = new Reduce();
			next = new HashMap<>();
			resolveAllReduces(next, nextx, reduce);
			BuildLr buildLr1 = buildLr(ps, eg.children.get(0), next);
			reduce.name = eg.content;
			reduce.n = buildLr1.nTokens;
			buildLr = new BuildLr(1, buildLr1.next);
			break;
		case OR____:
			state1 = newState(nextx);
			next = new HashMap<>();
			for (EbnfGrammar eg1 : Read.from(eg.children)) {
				String egn = "OR" + counter++;
				resolve(next, egn, Pair.of(state1, null));
				resolveAll(next, buildLr(ps, new EbnfGrammar(EbnfGrammarType.NAMED_, egn, eg1), nextx).next);
			}
			buildLr = new BuildLr(1, next);
			break;
		case STRING:
			buildLr = new BuildLr(1, kv(eg.content, Pair.of(newState(nextx), null)));
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return buildLr;
	}

	private State newState(Map<String, Pair<State, Reduce>> nextx) {
		State state = new State();
		fsm.put(state, nextx);
		return state;
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

	private boolean resolveAllReduces(Map<String, Pair<State, Reduce>> targetMap, Map<String, Pair<State, Reduce>> sourceMap,
			Reduce reduce) {
		State nullState = null;
		boolean b = false;
		for (Entry<String, Pair<State, Reduce>> e1 : sourceMap.entrySet())
			b |= resolve(targetMap, e1.getKey(), Pair.of(nullState, reduce));
		return b;
	}

	private boolean resolveAll(Map<String, Pair<State, Reduce>> targetMap, Map<String, Pair<State, Reduce>> sourceMap) {
		boolean b = false;
		for (Entry<String, Pair<State, Reduce>> e1 : sourceMap.entrySet())
			b |= resolve(targetMap, e1.getKey(), e1.getValue());
		return b;
	}

	// Shift-reduce conflict ends in reduce
	private boolean resolve(Map<String, Pair<State, Reduce>> map, String key, Pair<State, Reduce> value1) {
		Pair<State, Reduce> value0 = map.get(key);
		if (value0 == null || isShiftReduceConflict(value0, value1)) {
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

	private <K, V> Map<K, V> kv(K k, V v) {
		Map<K, V> m = new HashMap<>();
		m.put(k, v);
		return m;
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
