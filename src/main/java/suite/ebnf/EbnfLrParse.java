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
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private UnionFind<State> unionFind = new UnionFind<>();
	private List<Shift> shifts0 = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();

	private Map<String, Transition> transitionByEntity;
	private Map<Pair<String, State>, State> shifts;
	private Map<State, Pair<String, State>> reduces;

	private class Shift {
		private String input;
		private Transition t;

		private Shift(String input, State state0, State statex) {
			this.input = input;
			t = new Transition(state0, statex);
		}
	}

	private class Reduce {
		private String name;
		private Transition t;

		private Reduce(String name, State state0, State statex) {
			this.name = name;
			t = new Transition(state0, statex);
		}
	}

	private class Transition {
		private State state0, statex;

		private Transition(State state0, State statex) {
			this.state0 = state0;
			this.statex = statex;
		}
	}

	private class State {
		private int n;

		private State(int nc) {
			this.n = nc;
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity) {
		List<String> entities = Read.from(grammarByEntity) //
				.map(Pair::first_) //
				.toList();

		transitionByEntity = Read.from(entities) //
				.map(entity -> Pair.of(entity, new Transition(newState(), newState()))) //
				.collect(As::map);

		Read.from(entities) //
				.forEach(entity -> {
					EbnfGrammar eg = grammarByEntity.get(entity);
					Transition t = transitionByEntity.get(entity);
					converge(buildLr(eg, t.state0), t.statex);
				});

		shifts = Read.from(shifts0) //
				.toMap(shift -> Pair.of(shift.input, find(shift.t.state0)), shift -> find(shift.t.statex));

		reduces = Read.from(reduces0) //
				.toMap(reduce -> find(reduce.t.state0), reduce -> Pair.of(reduce.name, find(reduce.t.statex)));
	}

	@Override
	public Node check(String entity, String in) {
		return parse(entity, in);
	}

	@Override
	public Node parse(String entity, String in) {
		Transition t = transitionByEntity.get(entity);
		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();
		return parse(source, find(t.state0), find(t.statex));
	}

	private Node parse(Source<Node> tokens, State state0, State statex) {
		Deque<Node> stack = new ArrayDeque<>();
		Node token = tokens.source();
		State state = state0;
		State shift;
		Pair<String, State> reduce;

		for (;;)
			if (token != null && (shift = shifts.get(Pair.of(token.entity, state))) != null) {
				System.out.println("SHIFT " + token);
				Node token1 = tokens.source();
				if (token1 != null) {
					stack.push(token);
					token = token1;
				}
				state = shift;
			} else if ((reduce = reduces.get(state)) != null) {
				System.out.println("REDUCE" + reduce.t0);
				IList<Node> nodes = IList.cons(token, IList.end());
				for (int i = 1; i < state.n; i++)
					nodes = IList.cons(stack.pop(), nodes);
				token = new Node(reduce.t0, 0, 0, Read.from(nodes).toList());
				state = reduce.t1;
			} else if (state == statex && stack.isEmpty())
				return token;
			else
				throw new RuntimeException("Parse error at " + token);
	}

	private Streamlet<State> buildLr(EbnfGrammar eg, State state0) {
		Streamlet<State> statesx;

		switch (eg.type) {
		case AND___:
			statesx = Read.from(state0);
			for (EbnfGrammar child : eg.children)
				statesx = statesx.concatMap(state_ -> buildLr(child, state_));
			break;
		case ENTITY:
			Transition t = transitionByEntity.get(eg.content);
			unionFind.union(state0, t.state0);
			statesx = Read.from(t.statex);
			break;
		case NAMED_:
			State statex_ = newState();
			for (State state : buildLr(eg.children.get(0), state0))
				reduces0.add(new Reduce(eg.content, state, statex_));
			statesx = Read.from(statex_);
			break;
		case OPTION:
			statesx = buildLr(eg.children.get(0), state0).cons(state0);
			break;
		case OR____:
			statesx = Read.from(eg.children).concatMap(eg_ -> buildLr(eg_, state0));
			break;
		case REPT0_:
			converge(buildLr(eg.children.get(0), state0), state0);
			statesx = Read.from(state0);
			break;
		case STRING:
			State statex = newState(state0);
			shifts0.add(new Shift(eg.content, state0, statex));
			statesx = Read.from(statex);
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return statesx;
	}

	private void converge(Streamlet<State> states, State state) {
		for (State state_ : states)
			unionFind.union(state_, state);
	}

	private State find(State state) {
		return unionFind.find(state);
	}

	private State newState(State state) {
		return new State(state.n + 1);
	}

	private State newState() {
		return new State(0);
	}

}
