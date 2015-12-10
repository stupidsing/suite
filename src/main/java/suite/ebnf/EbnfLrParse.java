package suite.ebnf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private Map<State, String> references = new HashMap<>();
	private List<Shift> shifts0 = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();
	private int counter;

	private Map<String, State> stateByEntity;
	private Map<Pair<String, State>, Shift> shifts;
	private Map<State, Reduce> reduces;

	private class Shift {
		private String input;
		private State state0, statex;

		private Shift(String input, State state0, State statex) {
			this.input = input;
			this.state0 = state0;
			this.statex = statex;
		}

		public String toString() {
			return "(" + input + ", " + state0 + ") -> " + statex;
		}
	}

	private class Reduce {
		private String name;
		private int n;
		private State state;

		private Reduce(String name, Pair<Integer, State> pair) {
			this(name, pair.t0, pair.t1);
		}

		private Reduce(String name, int n, State state) {
			this.name = name;
			this.n = n;
			this.state = state;
		}

		public String toString() {
			return state + " -> " + name + "/" + n;
		}
	}

	private class State {
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity) {
		List<String> entities = Read.from(grammarByEntity) //
				.map(Pair::first_) //
				.toList();

		stateByEntity = Read.from(entities) //
				.map(entity -> {
					EbnfGrammar eg = grammarByEntity.get(entity);
					State state = new State();
					reduces0.add(new Reduce(eg.content, buildLr(eg, state)));
					return Pair.of(entity, state);
				}) //
				.collect(As::map);

		ListMultimap<State, Shift> shiftsByState = Read.from(shifts0).toMultimap(shift -> shift.state0);

		c: while (!references.isEmpty()) {
			for (Entry<State, String> entry : references.entrySet()) {
				State state1;

				if (!references.containsKey(state1 = stateByEntity.get(entry.getValue()))) {
					State state = entry.getKey();
					for (Shift shift : shiftsByState.get(state1))
						shiftsByState.put(state, new Shift(shift.input, state, shift.statex));
					references.remove(state);
					continue c;
				}
			}

			throw new RuntimeException();
		}

		shifts = Read.from(shiftsByState.entries()) //
				.map(Pair::second) //
				.toMap(shift -> Pair.of(shift.input, shift.state0));

		reduces = Read.from(reduces0).toMap(reduce -> reduce.state);
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
		System.out.println("shifts = " + shifts.values());
		System.out.println();
		System.out.println("reduces = " + reduces.values());
		System.out.println();
		System.out.println("Initial state = " + state);
		System.out.println();

		return parse(source, state, entity);
	}

	private Node parse(Source<Node> tokens, State state0, String entity) {
		Deque<Pair<Node, State>> stack = new ArrayDeque<>();
		State state = state0;
		Node token = tokens.source();
		Shift shift;
		Reduce reduce;

		while (true) {
			String lookahead = token.entity;
			System.out.print("(L=" + lookahead + ", S=" + state + ", Stack=" + stack.size() + ") ");

			if ((shift = shifts.get(Pair.of(lookahead, state))) != null) {
				System.out.print("SHIFT " + token);
				stack.push(Pair.of(token, state));
				state = shift.statex;

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
			statex = new State();
			shifts0.add(new Shift(eg.content, state0, statex));
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
			for (EbnfGrammar child : eg.children) {
				String entity1 = "OR" + counter++;
				reduces0.add(new Reduce(entity1, buildLr(child, state0)));
				shifts0.add(new Shift(entity1, state0, statex));
			}
			break;
		case STRING:
			nTokens = 1;
			statex = new State();
			shifts0.add(new Shift(eg.content, state0, statex));
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return Pair.of(nTokens, statex);
	}

}
