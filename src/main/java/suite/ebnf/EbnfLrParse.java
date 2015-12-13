package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private Map<State, String> references = new HashMap<>();
	private int counter;

	private Map<String, State> stateByEntity;
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
		List<String> entities = Read.from(grammarByEntity) //
				.map(Pair::first_) //
				.toList();

		stateByEntity = Read.from(entities) //
				.map(entity -> {
					EbnfGrammar eg = grammarByEntity.get(entity);
					State state = new State();
					addReduce(eg.content, buildLr(eg, state));
					return Pair.of(entity, state);
				}) //
				.collect(As::map);

		c: while (!references.isEmpty()) {
			for (Entry<State, String> e0 : references.entrySet()) {
				State sourceState = stateByEntity.get(e0.getValue());
				State targetState = e0.getKey();

				if (!references.containsKey(sourceState)) {
					for (Entry<String, State> e1 : shifts.get(sourceState).entrySet())
						put(shifts.computeIfAbsent(targetState, state -> new HashMap<>()), e1.getKey(), e1.getValue());
					references.remove(targetState);
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
		State state = stateByEntity.get(entity);
		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();

		System.out.println("transitionByEntity = " + stateByEntity);
		System.out.println();
		System.out.println("shifts = " + shifts);
		System.out.println();
		System.out.println("reduces = " + reduces);
		System.out.println();
		System.out.println("Initial state = " + state);
		System.out.println();

		return parse(source, state, entity);
	}

	private Node parse(Source<Node> tokens, State state0, String entity) {
		Deque<Pair<Node, State>> stack = new ArrayDeque<>();
		Map<String, State> m;
		State state = state0, state1;
		Node token = tokens.source();
		Reduce reduce;

		while (true) {
			String lookahead = token.entity;
			System.out.print("(L=" + lookahead + ", S=" + state + ", Stack=" + stack.size() + ") ");

			if ((m = shifts.get(state)) != null && (state1 = m.get(lookahead)) != null) {
				System.out.print("SHIFT " + token);
				stack.push(Pair.of(token, state));
				state = state1;

				if ((reduce = reduces.get(state)) != null) {
					System.out.print(", REDUCE " + reduce.name + "/" + reduce.n);
					Pair<Node, State> pair = null;
					IList<Node> nodes = IList.end();
					for (int i = 0; i < reduce.n; i++)
						nodes = IList.cons((pair = stack.pop()).t0, nodes);
					state = pair.t1;
					token = new Node(reduce.name, 0, 0, Read.from(nodes).toList());
				} else
					token = tokens.source();
			} else if (entity.equals(lookahead) && stack.size() == 0 && tokens.source() == null)
				return token;
			else
				throw new RuntimeException("Parse error at " + token);

			System.out.println();
		}
	}

	private Pair<Integer, State> buildLr(EbnfGrammar eg, State state0) {
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
			statex = getShiftMap(state0).computeIfAbsent(eg.content, content -> new State());
			references.put(state0, eg.content);
			break;
		case NAMED_:
			Pair<Integer, State> pair = buildLr(eg.children.get(0), state0);
			nTokens = pair.t0;
			statex = pair.t1;
			break;
		case OR____:
			nTokens = 1;
			statex = new State();
			Map<String, State> m = getShiftMap(state0);
			for (EbnfGrammar child : eg.children) {
				String entity1 = "OR" + counter++;
				addReduce(entity1, buildLr(child, state0));
				put(m, entity1, statex);
			}
			break;
		case STRING:
			nTokens = 1;
			statex = getShiftMap(state0).computeIfAbsent(eg.content, content -> new State());
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

	private <K, V> void put(Map<K, V> map, K key, V value) {
		if (map.get(key) == null)
			map.put(key, value);
		else
			throw new RuntimeException();
	}

}
