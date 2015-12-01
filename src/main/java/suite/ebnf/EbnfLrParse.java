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
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public class EbnfLrParse implements EbnfParse {

	private UnionFind<State> unionFind = new UnionFind<>();
	private List<Rule> rules = new ArrayList<>();
	private List<Reduce> reduces0 = new ArrayList<>();

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
		private int n;

		private State(int nc) {
			this.n = nc;
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarsByEntity, EbnfGrammar root) {
		for (EbnfGrammar eg : grammarsByEntity.values())
			buildLr(eg, newState());

		fsm = Read.from(rules) //
				.toMap(rule -> Pair.of(rule.input, unionFind.find(rule.state0)), rule -> unionFind.find(rule.statex));

		reduces = Read.from(reduces0) //
				.toMap(reduce -> unionFind.find(reduce.state0), reduce -> reduce);
	}

	@Override
	public Node check(EbnfGrammar eg, String in) {
		return parse(eg, in);
	}

	@Override
	public Node parse(EbnfGrammar eg, String in) {
		State state0 = newState();
		State statex = newState();

		for (State state : buildLr(eg, state0))
			unionFind.union(state, statex);
		return parse(new Lexer(in).tokens(), state0, statex);
	}

	private Node parse(Source<String> tokens, State state0, State statex) {
		Deque<Node> stack = new ArrayDeque<>();
		State state = state0;
		Reduce reduce;
		String token;

		while ((token = tokens.source()) != null && state != null) {
			stack.push(new Node(token, 0));
			state = fsm.get(Pair.of(token, state));

			if ((reduce = reduces.get(state)) != null) {
				IList<Node> nodes = IList.end();

				for (int i = 0; i < state.n; i++)
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
		return new State(state.n + 1);
	}

	private State newState() {
		return new State(0);
	}

}
