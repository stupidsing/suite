package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private int counter;
	private Map<String, Pair<State, State>> transitionByEntity = new HashMap<>();
	private Map<State, Map<String, State>> shifts = new HashMap<>();
	private Map<State, Reduce> reduces = new HashMap<>();

	private class Reduce {
		private String name;
		private int n;

		private Reduce(String name, int n) {
			this.name = name;
			this.n = n;
		}

		public String toString() {
			return name + "/" + n;
		}
	}

	private class State {
	}

	public static EbnfLrParse of(String grammar) {
		try (StringReader reader = new StringReader(grammar)) {
			return new EbnfLrParse(EbnfGrammar.parse(reader));
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity) {
		for (EbnfGrammar eg : grammarByEntity.values())
			buildLr(eg, new State());

		Streamlet<Pair<State, State>> ss0 = Read.from(shifts) //
				.concatMapValue(v -> Read.from(v.keySet())) //
				.mapValue(transitionByEntity::get) //
				.filter((k, v) -> v != null) //
				.map((k, v) -> Pair.of(k, v.t0));

		Streamlet<Pair<State, State>> ss1 = Read.from(shifts) //
				.concatMap2((k, v) -> Read.from(v)) //
				.mapKey(transitionByEntity::get) //
				.filter((k, v) -> k != null) //
				.map((k, v) -> Pair.of(k.t1, v));

		ListMultimap<State, State> merges = Streamlet.concat(ss0, ss1).toMultimap(Pair::first_, Pair::second);

		c: while (!merges.isEmpty()) {
			for (Pair<State, State> e0 : merges.entries()) {
				State sourceState = e0.t1;
				State targetState = e0.t0;
				boolean b = false;

				if (sourceState == targetState)
					b = true;
				else if (merges.get(sourceState).isEmpty()) {
					Map<String, State> sourceShiftMap = getShiftMap(sourceState);
					Map<String, State> targetShiftMap = getShiftMap(targetState);
					for (Entry<String, State> e1 : sourceShiftMap.entrySet())
						put(targetShiftMap, e1.getKey(), e1.getValue());
					b = true;
				} else
					b = false;

				if (b) {
					merges.remove(targetState, sourceState);
					continue c;
				}
			}

			throw new RuntimeException();
		}
	}

	@Override
	public Node check(String entity, String in) {
		return parse(entity, in);
	}

	@Override
	public Node parse(String entity, String in) {
		State state = transitionByEntity.get(entity).t0;
		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();

		System.out.println("transitionByEntity = " + list(transitionByEntity));
		System.out.println("shifts = " + list(shifts));
		System.out.println("reduces = " + list(reduces));
		System.out.println("Initial state = " + state);

		return parse(source, state, entity);
	}

	private Node parse(Source<Node> tokens, State state0, String entity) {
		Deque<Pair<Node, State>> stack = new ArrayDeque<>();
		Node token = tokens.source();
		State state = state0;

		while (true) {
			State shift;

			// Shift as much as possible
			while (token != null && (shift = shift(state, token, stack)) != null) {
				state = shift;
				token = tokens.source();
			}

			// Reduce
			Reduce reduce = reduces.get(state);
			IList<Node> nodes = IList.end();
			System.out.println("(S=" + state + ", Stack=" + stack.size() + "), REDUCE " + reduce.name + "/" + reduce.n);

			for (int i = 0; i < reduce.n; i++) {
				Pair<Node, State> pair = stack.pop();
				nodes = IList.cons(pair.t0, nodes);
				state = pair.t1;
			}

			Node token1 = new Node(reduce.name, 0, 0, Read.from(nodes).toList());

			// Force shift after reduce
			if (entity.equals(reduce.name) && stack.size() == 0 && token == null)
				return token1;
			else if ((shift = shift(state, token1, stack)) != null)
				state = shift;
			else
				throw new RuntimeException();
		}
	}

	private State shift(State state, Node token, Deque<Pair<Node, State>> stack) {
		String lookahead = token.entity;
		Map<String, State> m;
		State state1;

		if ((m = shifts.get(state)) != null && (state1 = m.get(lookahead)) != null) {
			System.out.println("(S=" + state + ", Stack=" + stack.size() + "), SHIFT " + lookahead);
			stack.push(Pair.of(token, state));
			return state1;
		} else
			return null;
	}

	private Pair<Integer, State> buildLr(EbnfGrammar eg, State state0) {
		Map<String, State> shiftMap = getShiftMap(state0);

		BiConsumer<String, State> reducer = (entity1, statex_) -> {
			for (EbnfGrammar child : eg.children)
				addReduce(entity1, buildLr(child, state0));
			transitionByEntity.put(entity1, Pair.of(state0, statex_));
		};

		int nTokens;
		State statex;

		switch (eg.type) {
		case AND___:
			nTokens = 0;
			statex = state0;
			for (EbnfGrammar child : eg.children) {
				Pair<Integer, State> p = buildLr(child, statex);
				nTokens += p.t0;
				statex = p.t1;
			}
			break;
		case ENTITY:
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(eg.content, content -> new State());
			break;
		case NAMED_:
			nTokens = 1;
			statex = new State();
			reducer.accept(eg.content, statex);
			break;
		case OR____:
			String entity1 = "OR" + counter++;
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(entity1, content -> new State());
			reducer.accept(entity1, statex);
			break;
		case STRING:
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(eg.content, content -> new State());
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return Pair.of(nTokens, statex);
	}

	private Map<String, State> getShiftMap(State state0) {
		return shifts.computeIfAbsent(state0, state -> new HashMap<>());
	}

	private void addReduce(String entity, Pair<Integer, State> pair) {
		put(reduces, pair.t1, new Reduce(entity, pair.t0));
	}

	private <K, V> void put(Map<K, V> map, K key, V value1) {
		V value0 = map.get(key);
		if (value0 == null)
			map.put(key, value1);
		else if (value0 != value1)
			throw new RuntimeException("Duplicate key " + key);
	}

	public <K, V> String list(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		for (Entry<K, V> pair : map.entrySet())
			sb.append(pair.getKey() + " = " + pair.getValue() + "\n");
		sb.append("}\n");
		return sb.toString();
	}

}
