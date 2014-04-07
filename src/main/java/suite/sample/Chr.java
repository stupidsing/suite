package suite.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.immutable.IMap;
import suite.immutable.ISet;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Rewriter;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;

/**
 * Constraint handling rules implementation.
 * 
 * @author ywsing
 */
public class Chr {

	private List<Rule> rules = new ArrayList<>();
	private Prover prover = new Prover(Suite.createRuleSet());

	private class Rule {
		private List<Node> givens = new ArrayList<>();
		private List<Node> ifs = new ArrayList<>();
		private List<Node> thens = new ArrayList<>();
		private Node when = Atom.NIL;
	}

	private class State {
		private IMap<Prototype, ISet<Node>> factsByPrototype;

		public State(IMap<Prototype, ISet<Node>> factsByPrototype) {
			this.factsByPrototype = factsByPrototype;
		}
	}

	public void addRule(Node node) {
		Rule rule = new Rule();

		while (node != Atom.create("end")) {
			Tree t0 = Tree.decompose(node, TermOp.TUPLE_);
			Tree t1 = t0 != null ? Tree.decompose(t0.getRight(), TermOp.TUPLE_) : null;

			if (t1 != null) {
				Node key = t0.getLeft();
				Node value = t1.getLeft();
				node = t1.getRight();

				if (key == Atom.create("given"))
					rule.givens = To.list(Tree.iter(value));
				else if (key == Atom.create("if"))
					rule.ifs = To.list(Tree.iter(value));
				else if (key == Atom.create("then"))
					rule.thens = To.list(Tree.iter(value));
				else if (key == Atom.create("when"))
					rule.when = value;
				else
					throw new RuntimeException("Invalid key " + key);
			} else
				throw new RuntimeException("Invalid rule " + node);
		}

		rules.add(rule);
	}

	public Collection<Node> chr(Collection<Node> facts) {
		State state = new State(new IMap<Prototype, ISet<Node>>());

		for (Node fact : facts) {
			Prototype prototype = getPrototype(fact);
			state = setFacts(state, prototype, getFacts(state, prototype).replace(fact));
		}

		State state1;

		while ((state1 = chr(state)) != null)
			state = state1;

		List<Node> nodes1 = new ArrayList<>();

		for (Pair<Prototype, ISet<Node>> pair : state.factsByPrototype)
			nodes1.addAll(To.list(pair.t1));

		return nodes1;
	}

	private State chr(final State state) {
		return FunUtil.concat(map(To.source(rules), new Fun<Rule, Source<State>>() {
			public Source<State> apply(Rule rule) {
				return chr(state, rule);
			}
		})).source();
	}

	private Source<State> chr(final State state, Rule rule) {
		Generalizer generalizer = new Generalizer();
		final Journal journal = new Journal();
		Source<State> states = To.source(state);

		for (Node if_ : rule.ifs)
			states = chrIf(states, journal, generalizer.generalize(if_));

		for (Node given : rule.givens)
			states = chrGiven(states, journal, generalizer.generalize(given));

		states = chrWhen(states, generalizer.generalize(rule.when));

		for (Node then : rule.thens)
			states = chrThen(states, generalizer.generalize(then));

		return states;
	}

	private Source<State> chrIf(Source<State> states, final Journal journal, final Node if_) {
		final Prototype prototype = getPrototype(if_);

		return FunUtil.concat(map(states, new Fun<State, Source<State>>() {
			public Source<State> apply(final State state) {
				final ISet<Node> facts = getFacts(state, prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, if_);

				Source<Node> bindedIfs = filter(facts.source(), bindFun);
				return map(bindedIfs, new Fun<Node, State>() {
					public State apply(Node node) {
						return setFacts(state, prototype, facts.remove(node));
					}
				});
			}
		}));
	}

	private Source<State> chrGiven(Source<State> states, final Journal journal, final Node given) {
		final Prototype prototype = getPrototype(given);

		return FunUtil.concat(map(states, new Fun<State, Source<State>>() {
			public Source<State> apply(final State state) {
				ISet<Node> facts = getFacts(state, prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, given);
				boolean isMatch = or(map(facts.source(), bindFun));
				return isMatch ? To.source(state) : FunUtil.<State> nullSource();
			}
		}));
	}

	private Source<State> chrThen(Source<State> states, final Node then) {
		Generalizer generalizer = new Generalizer();
		Node a = atom(".a"), b = atom(".b");

		if (Binder.bind(then, generalizer.generalize(Suite.substitute(".0 = .1", a, b)), new Journal())) {

			// Built-in syntactic equality
			final Reference from = generalizer.getVariable(a);
			final Reference to = generalizer.getVariable(b);

			states = map(states, new Fun<State, State>() {
				public State apply(State state) {
					IMap<Prototype, ISet<Node>> factsByPrototype1 = new IMap<>();
					for (Pair<Prototype, ISet<Node>> pair : state.factsByPrototype)
						factsByPrototype1 = factsByPrototype1.put(pair.t0, replace(pair.t1));
					return new State(factsByPrototype1);
				}

				private ISet<Node> replace(ISet<Node> facts) {
					ISet<Node> facts1 = new ISet<Node>();
					for (Node node : facts)
						facts1 = facts1.replace(new Rewriter(from, to).replace(node));
					return facts1;
				}
			});
		}

		return map(states, new Fun<State, State>() {
			public State apply(State state) {
				Prototype prototype = getPrototype(then);
				ISet<Node> facts = getFacts(state, prototype);
				return setFacts(state, prototype, facts.replace(then));
			}
		});
	}

	private Source<State> chrWhen(Source<State> states, final Node when) {
		return filter(states, new Fun<State, Boolean>() {
			public Boolean apply(State state) {
				return prover.prove(when);
			}
		});
	}

	private Fun<Node, Boolean> bindFun(final Journal journal, final Node node0) {
		final int pit = journal.getPointInTime();

		return new Fun<Node, Boolean>() {
			public Boolean apply(Node node1) {
				journal.undoBinds(pit);
				return Binder.bind(node0, node1, journal);
			}
		};
	}

	private boolean or(Source<Boolean> source) {
		Boolean b;
		while ((b = source.source()) != null)
			if (b == Boolean.TRUE)
				return true;
		return false;
	}

	private <T> Source<T> filter(Source<T> source, Fun<T, Boolean> fun) {
		return FunUtil.filter(fun, source);
	}

	private <T0, T1> Source<T1> map(Source<T0> source, Fun<T0, T1> fun) {
		return FunUtil.map(fun, source);
	}

	private Node atom(String name) {
		return Atom.create(name);
	}

	private ISet<Node> getFacts(State state, Prototype prototype) {
		ISet<Node> results = state.factsByPrototype.get(prototype);
		return results != null ? results : new ISet<Node>();
	}

	private State setFacts(State state, Prototype prototype, ISet<Node> nodes) {
		IMap<Prototype, ISet<Node>> facts = state.factsByPrototype;
		return new State(nodes.source().source() != null ? facts.replace(prototype, nodes) : facts.remove(prototype));
	}

	private Prototype getPrototype(Node node) {
		return Prototype.get(node);
	}

}
