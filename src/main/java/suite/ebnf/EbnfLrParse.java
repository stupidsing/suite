package suite.ebnf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.algo.UnionFind;
import suite.ebnf.Ebnf.Node;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private UnionFind<State> unionFind = new UnionFind<>();
	private List<Shift> shifts0 = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();

	private Map<String, Transition> transitionByEntity;
	private Map<Pair<String, State>, Shift> shifts;
	private Map<State, Reduce> reduces;

	private class Shift {
		private String input;
		private State state0, statex;

		private Shift(Shift shift) {
			this(shift.input, find(shift.state0), find(shift.statex));
		}

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
		private State state0, statex;

		private Reduce(Reduce reduce) {
			this(reduce.name, reduce.n, find(reduce.state0), find(reduce.statex));
		}

		private Reduce(String name, Pair<Integer, State> pair, State statex) {
			this(name, pair.t0, pair.t1, statex);
		}

		private Reduce(String name, int n, State state0, State statex) {
			this.name = name;
			this.n = n;
			this.state0 = state0;
			this.statex = statex;
		}

		public String toString() {
			return state0 + " -> (" + name + "/" + n + ", " + statex + ")";
		}
	}

	private class Transition {
		private State state0 = new State();
		private State statex = new State();

		public String toString() {
			return find(state0) + " -> " + find(statex);
		}
	}

	private class State {
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity) {
		List<String> entities = Read.from(grammarByEntity) //
				.map(Pair::first_) //
				.toList();

		transitionByEntity = Read.from(entities) //
				.map(entity -> Pair.of(entity, new Transition())) //
				.collect(As::map);

		Read.from(entities).forEach(entity -> {
			EbnfGrammar eg = grammarByEntity.get(entity);
			Transition t = transitionByEntity.get(entity);
			Pair<Integer, State> pair = buildLr(eg, t.state0);
			if (pair.t0 == 1)
				unionFind.union(pair.t1, t.statex);
			else
				throw new RuntimeException();
		});

		shifts = Read.from(shifts0) //
				.map(Shift::new) //
				.toMap(shift -> Pair.of(shift.input, shift.state0));

		reduces = Read.from(reduces0) //
				.map(Reduce::new) //
				.toMap(reduce -> reduce.state0);
	}

	@Override
	public Node check(String entity, String in) {
		return parse(entity, in);
	}

	@Override
	public Node parse(String entity, String in) {
		Transition t = transitionByEntity.get(entity);
		State state0 = find(t.state0);
		State statex = find(t.statex);

		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();

		System.out.println("transitionByEntity = " + transitionByEntity);
		System.out.println();
		System.out.println("shifts = " + shifts.values());
		System.out.println();
		System.out.println("reduces = " + reduces.values());
		System.out.println();
		System.out.println("FROM " + state0 + " TO " + statex);
		System.out.println();

		return parse(source, state0, statex);
	}

	private Node parse(Source<Node> tokens, State state0, State statex) {
		Deque<Node> stack = new ArrayDeque<>();
		Runnable shiftToken = () -> {
			Node token = tokens.source();
			if (token != null)
				stack.push(token);
		};
		State state = state0;
		Shift shift;
		Reduce reduce;

		shiftToken.run();

		for (;;) {
			Node top = stack.peek();

			if (top != null && (shift = shifts.get(Pair.of(top.entity, state))) != null) {
				System.out.println("SHIFT " + top);
				shiftToken.run();
				state = shift.statex;
			} else if ((reduce = reduces.get(state)) != null) {
				System.out.println("REDUCE " + reduce.name + "/" + reduce.n);
				IList<Node> nodes = IList.end();
				for (int i = 0; i < reduce.n; i++)
					nodes = IList.cons(stack.pop(), nodes);
				stack.push(new Node(reduce.name, 0, 0, Read.from(nodes).toList()));
				state = reduce.statex;
			} else if (state == statex && stack.size() == 1)
				return top;
			else
				throw new RuntimeException("Parse error at " + top);
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
			Transition t = transitionByEntity.get(eg.content);
			unionFind.union(state0, t.state0);
			unionFind.union(statex, t.statex);
			break;
		case NAMED_:
			nTokens = 1;
			statex = new State();
			reduces0.add(new Reduce(eg.content, buildLr(eg.children.get(0), state0), statex));
			break;
		case OR____:
			nTokens = 1;
			statex = new State();
			for (EbnfGrammar child : eg.children)
				reduces0.add(new Reduce("OR", buildLr(child, state0), statex));
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

	private State find(State state) {
		return unionFind.find(state);
	}

}
