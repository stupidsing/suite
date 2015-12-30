package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private int counter;
	private String rootEntity;
	private Map<String, EbnfGrammar> grammarByEntity;
	private Set<Pair<String, Set<String>>> instances = new HashSet<>();
	private Map<Pair<String, Set<String>>, BuildLr> buildLrs = new HashMap<>();

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
		getLookaheadSet(eg, Read.empty(), IList.end());

		Pair<State, Reduce> pair = Pair.of(null, null);
		Read.from2(instances).sink((entity, lookaheads) -> {
			Map<String, Pair<State, Reduce>> next = Read.from(lookaheads).map2(l -> l, l -> pair).toMap();
			buildLr(grammarByEntity.get(entity), next);
		});

		fsm.put(state0 = new State(), buildLr(eg, kv("EOF", Pair.of(new State(), null))).next);
	}

	private Streamlet<String> getLookaheadSet(EbnfGrammar eg, Streamlet<String> lsx, IList<Pair<String, Set<String>>> ps) {
		Streamlet<String> ls0;

		switch (eg.type) {
		case AND___:
			if (!eg.children.isEmpty()) {
				EbnfGrammar tail = new EbnfGrammar(EbnfGrammarType.AND___, Util.right(eg.children, 1));
				Streamlet<String> ls1 = getLookaheadSet(tail, lsx, ps);
				ls0 = getLookaheadSet(eg.children.get(0), ls1, ps);
			} else
				ls0 = lsx;
			break;
		case ENTITY:
			Pair<String, Set<String>> p = Pair.of(eg.content, lsx.toSet());
			instances.add(p);
			Streamlet<String> st = !ps.contains(p) //
					? getLookaheadSet(grammarByEntity.get(eg.content), lsx, IList.cons(p, ps)) //
					: Read.empty();
			ls0 = st.cons(eg.content);
			break;
		case NAMED_:
			ls0 = getLookaheadSet(eg.children.get(0), lsx, ps);
			break;
		case OR____:
			Streamlet<String> lsx1 = lsx.memoize();
			ls0 = Read.from(eg.children).concatMap(eg1 -> getLookaheadSet(eg1, lsx1, ps));
			break;
		case STRING:
			ls0 = Read.from(eg.content);
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return ls0;
	}

	private BuildLr buildLr(EbnfGrammar eg, Map<String, Pair<State, Reduce>> nextx) {
		BuildLr buildLr;

		switch (eg.type) {
		case AND___: {
			int nTokens = 0;
			Map<String, Pair<State, Reduce>> next;
			next = nextx;
			for (EbnfGrammar eg1 : Read.from(eg.children).reverse()) {
				BuildLr buildLr1 = buildLr(eg1, next);
				nTokens += buildLr1.nTokens;
				next = buildLr1.next;
			}
			buildLr = new BuildLr(nTokens, next);
			break;
		}
		case ENTITY: {
			Pair<String, Set<String>> pair = Pair.of(eg.content, nextx.keySet());
			BuildLr buildLr1 = buildLrs.computeIfAbsent(pair, pair_ -> buildLr(grammarByEntity.get(eg.content), nextx));
			State state1 = new State();
			fsm.put(state1, nextx);
			Map<String, Pair<State, Reduce>> next = kv(eg.content, Pair.of(state1, null));
			resolveAll(next, buildLr1.next);
			buildLr = new BuildLr(buildLr1.nTokens, next);
			break;
		}
		case NAMED_: {
			State nullState = null;
			Reduce reduce = new Reduce();
			Map<String, Pair<State, Reduce>> next1 = Read.from(nextx).mapValue(lookahead -> Pair.of(nullState, reduce)).toMap();
			BuildLr buildLr1 = buildLr(eg.children.get(0), next1);
			reduce.name = eg.content;
			reduce.n = buildLr1.nTokens;
			buildLr = new BuildLr(1, buildLr1.next);
			break;
		}
		case OR____: {
			State state1 = new State();
			fsm.put(state1, nextx);
			int nTokens = 1;
			Map<String, Pair<State, Reduce>> next = new HashMap<>();
			for (EbnfGrammar eg1 : Read.from(eg.children)) {
				String egn = "OR" + counter++;
				BuildLr buildLr1 = buildLr(new EbnfGrammar(EbnfGrammarType.NAMED_, egn, eg1), nextx);
				resolve(next, egn, Pair.of(state1, null));
				resolveAll(next, buildLr1.next);
			}
			buildLr = new BuildLr(nTokens, next);
			break;
		}
		case STRING: {
			State state1 = new State();
			fsm.put(state1, nextx);
			buildLr = new BuildLr(1, kv(eg.content, Pair.of(state1, null)));
			break;
		}
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return buildLr;
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
