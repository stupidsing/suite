package suite.ebnf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
import suite.algo.UnionFind;
import suite.ebnf.Ebnf.Node;
import suite.immutable.IList;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private UnionFind<State> unionFind = new UnionFind<>();
	private List<Rule> rules = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();

	private State state0, statex;
	private Map<Pair<String, State>, State> fsm;
	private Map<State, Reduce> reduces;

	private class Rule {
		private String input;
		private State state0, statex;

		private Rule(String input, State state0, State statex) {
			this.input = input;
			this.state0 = state0;
			this.statex = statex;
		}
	}

	private class Reduce {
		private String name;
		private State state0, statex;

		private Reduce(String name, State state0, State statex) {
			this.name = name;
			this.state0 = state0;
			this.statex = statex;
		}
	}

	private class State {
		private int nc;

		private State(int nc) {
			this.nc = nc;
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarsByEntity, String rootEntity) {
		state0 = newState();
		statex = newState();

		for (Entry<String, EbnfGrammar> entry : grammarsByEntity.entrySet()) {
			Streamlet<State> states = buildLr(entry.getValue(), state0);
			if (Util.stringEquals(entry.getKey(), rootEntity))
				for (State state : states)
					unionFind.union(state, statex);
		}

		fsm = Read.from(rules) //
				.toMap(rule -> Pair.of(rule.input, unionFind.find(rule.state0)), rule -> unionFind.find(rule.statex));

		reduces = Read.from(reduces0) //
				.toMap(reduce -> unionFind.find(reduce.state0), reduce -> reduce);
	}

	public Node parse(Source<String> tokens) {
		Deque<Node> stack = new ArrayDeque<>();
		State state = state0;
		Reduce reduce;
		String token;

		while ((token = tokens.source()) != null && state != null) {
			stack.push(new Node(token, 0));
			state = fsm.get(Pair.of(token, state));

			if ((reduce = reduces.get(state)) != null) {
				IList<Node> nodes = IList.end();

				for (int i = 0; i < state.nc; i++)
					nodes = IList.cons(stack.pop(), nodes);

				stack.push(new Node(reduce.name, 0, 0, Read.from(nodes).toList()));
				state = reduce.statex;
			}
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
			statesx = buildLr(eg.children.get(0), state0);
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
			for (State statex : buildLr(eg.children.get(0), state0))
				unionFind.union(state0, statex);
			statesx = Read.from(state0);
			break;
		case STRING:
			State statex = newState(state0);
			rules.add(new Rule(eg.content, state0, statex));
			statesx = Read.from(statex);
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return statesx;
	}

	private State newState(State state) {
		return new State(state.nc + 1);
	}

	private State newState() {
		return new State(0);
	}

}
