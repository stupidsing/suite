package suite.algo;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.adt.map.ListMultimap;
import primal.fp.Funs.Fun;
import primal.streamlet.Streamlet;

// https://www.geeksforgeeks.org/program-implement-nfa-epsilon-move-dfa-conversion/
public class NfaToDfaTest {

	public class NfaToDfa<Symbol> {
		private char ch = 'A';
		private Symbol eps;

		public class State {
			public final char name = ch++;
			public final boolean term;

			public State() {
				this(false);
			}

			public State(boolean term) {
				this.term = term;
			}

			public String toString() {
				return Character.toString(name);
			}
		}

		public class Nfa {
			public final State init;
			public final Map<State, ListMultimap<Symbol, State>> edges = new HashMap<>();

			public Nfa(State init) {
				this.init = init;
			}

			public void add(State from, Symbol edge, State to) {
				edges.computeIfAbsent(from, s -> new ListMultimap<>()).put(edge, to);
			}
		}

		public class Dfa {
			public final State init;
			public final Map<State, Map<Symbol, State>> edges;

			public Dfa(State init, Map<State, Map<Symbol, State>> edges) {
				this.init = init;
				this.edges = edges;
			}

			public void add(State from, Symbol edge, State to) {
				edges.computeIfAbsent(from, s -> new HashMap<>()).put(edge, to);
			}

			public String toString() {
				var edges_ = Read.from2(edges);

				return "init = " + init + ", edges = " + Streamlet.concat( //
						edges_ //
								.keys() //
								.map(state -> "\n" + state + (state.term ? " (terminal)" : "")), //
						edges_ //
								.concatMap((fr, m) -> Read //
										.from2(m) //
										.map((st, to) -> "\n" + fr + " [" + st + "] => " + to)))
						.toJoinedString();
			}
		}

		private NfaToDfa(Symbol eps) {
			this.eps = eps;
		}

		private Dfa toDfa(Nfa nfa) {
			Fun<State, Set<State>> closure = nfaState -> {
				var result = new HashSet<State>();
				var stack = new ArrayDeque<State>();
				stack.push(nfaState);
				while (!stack.isEmpty()) {
					var nfaState_ = stack.pop();
					if (result.add(nfaState_))
						nfa.edges.get(nfaState_).get(eps).forEach(stack::push);
				}
				return result;
			};

			var nfaStates0 = closure.apply(nfa.init);
			var stateMap = new HashMap<Set<State>, State>();
			var stack = new ArrayDeque<Set<State>>();

			var edges = new HashMap<State, Map<Symbol, State>>();

			Fun<Set<State>, State> addState = nfaStates -> {
				var term = Read.from(nfaStates).isAny(nfaState -> nfaState.term);
				var dfaState = new State(term);
				stateMap.put(nfaStates, dfaState);
				edges.put(dfaState, new HashMap<>());
				stack.push(nfaStates);
				return dfaState;
			};

			var init = addState.apply(nfaStates0);

			while (!stack.isEmpty()) {
				var nfaStates = stack.pop();
				var nfaEdges = Read.from(nfaStates).map(nfa.edges::get);
				var symbols1 = nfaEdges.flatMap(ListMultimap::keySet).filter(s -> !eps.equals(s)).distinct().toSet();
				var dfaState = stateMap.get(nfaStates);

				for (var symbol1 : symbols1) {
					var nfaStates1 = nfaEdges //
							.flatMap(nfaEdge -> nfaEdge.get(symbol1)) //
							.flatMap(closure::apply) //
							.distinct() //
							.toSet();

					var dfaState1 = stateMap.get(nfaStates1);

					if (dfaState1 == null)
						dfaState1 = addState.apply(nfaStates1);

					edges.get(dfaState).put(symbol1, dfaState1);
				}
			}

			return new Dfa(init, edges);
		}
	}

	@Test
	public void test() {
		var eps = "";

		var n2d = new NfaToDfa<String>(eps);

		var a = n2d.new State();
		var b = n2d.new State();
		var c = n2d.new State(true);
		var nfa = n2d.new Nfa(a);
		nfa.add(a, eps, b);
		nfa.add(a, "0", b);
		nfa.add(a, "0", c);
		nfa.add(a, "1", a);
		nfa.add(b, eps, c);
		nfa.add(b, "1", b);
		nfa.add(c, "0", c);
		nfa.add(c, "1", c);
		var dfa = n2d.toDfa(nfa);
		System.out.println("DFA = " + dfa);
	}

}
