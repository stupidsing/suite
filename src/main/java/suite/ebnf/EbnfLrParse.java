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
	private Map<String, Transition> transitionByEntity;
	private List<Shift> shifts0 = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();

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
					converge(buildLr(eg, t.state0).cons(t.statex));
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
		State state = state0;
		Pair<String, State> reduce;
		Node token;

		while ((token = tokens.source()) != null && state != null)
			while (true) {
				stack.push(token);
				state = shifts.get(Pair.of(token.entity, state));

				if ((reduce = reduces.get(state)) != null) {
					IList<Node> nodes = IList.end();

					for (int i = 0; i < state.n; i++)
						nodes = IList.cons(stack.pop(), nodes);

					token = new Node(reduce.t0, 0, 0, Read.from(nodes).toList());
					state = reduce.t1;
				} else
					break;
			}

		return token == null && state.equals(statex) && stack.size() == 1 ? stack.getFirst() : null;
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
			statesx = Read.from(converge(buildLr(eg.children.get(0), state0).cons(state0)));
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

	private State converge(Streamlet<State> states) {
		State statex = newState();
		for (State state : states)
			unionFind.union(state, statex);
		return statex;
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
