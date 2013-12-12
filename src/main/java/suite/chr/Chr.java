package suite.chr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.immutable.ImmutableMap;
import suite.immutable.ImmutableSet;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.Replacer;
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
		private Node when;
	}

	private class State {
		private ImmutableMap<Prototype, ImmutableSet<Node>> facts;

		public State(ImmutableMap<Prototype, ImmutableSet<Node>> facts) {
			this.facts = facts;
		}
	}

	public void addRule(Node node) {
		Node v0 = atom(".givens"), v1 = atom(".ifs"), v2 = atom(".thens"), v3 = atom(".when");

		Journal journal = new Journal();
		Generalizer generalizer = new Generalizer();
		Node bind = Suite.substitute("given .0 if .1 then .2 when .3 end", v0, v1, v2, v3);

		if (Binder.bind(generalizer.generalize(bind), node, journal)) {
			Rule rule = new Rule();
			rule.givens = To.list(Node.iter(generalizer.getVariable(v0)));
			rule.ifs = To.list(Node.iter(generalizer.getVariable(v1)));
			rule.thens = To.list(Node.iter(generalizer.getVariable(v2)));
			rule.when = generalizer.getVariable(v3);
			rules.add(rule);
		} else
			throw new RuntimeException("Invalid rule " + node);
	}

	public Collection<Node> chr(Collection<Node> nodes) {
		State state = new State(new ImmutableMap<Prototype, ImmutableSet<Node>>());

		for (Node node : nodes) {
			Prototype prototype = getPrototype(node);
			state = setFacts(state, prototype, getFacts(state, prototype).replace(node));
		}

		State state1;

		while ((state1 = chr(state)) != null)
			state = state1;

		List<Node> nodes1 = new ArrayList<>();

		for (Pair<Prototype, ImmutableSet<Node>> pair : state.facts)
			nodes1.addAll(To.list(pair.t1));

		return nodes1;
	}

	private State chr(final State state) {
		return FunUtil.concat(map(FunUtil.asSource(rules), new Fun<Rule, Source<State>>() {
			public Source<State> apply(Rule rule) {
				return chr(state, rule);
			}
		})).source();
	}

	private Source<State> chr(final State state, Rule rule) {
		Generalizer generalizer = new Generalizer();
		final Journal journal = new Journal();
		Source<State> states = FunUtil.asSource(state);

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
				final ImmutableSet<Node> facts = getFacts(state, prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, if_);
				Source<Node> bindedIfs = filter(FunUtil.asSource(facts), bindFun);

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
				ImmutableSet<Node> facts = getFacts(state, prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, given);
				boolean isMatch = or(map(FunUtil.asSource(facts), bindFun));
				return isMatch ? FunUtil.asSource(state) : FunUtil.<State> nullSource();
			}
		}));
	}

	private Source<State> chrThen(Source<State> states, final Node then) {
		Generalizer generalizer = new Generalizer();
		Node a = atom(".a"), b = atom(".b");

		if (Binder.bind(then, generalizer.generalize(Suite.substitute(".0 = .1", a, b)), new Journal())) {
			final Reference from = generalizer.getVariable(a);
			final Reference to = generalizer.getVariable(b);

			states = map(states, new Fun<State, State>() {
				public State apply(State state) {
					ImmutableMap<Prototype, ImmutableSet<Node>> facts1 = new ImmutableMap<>();

					for (Pair<Prototype, ImmutableSet<Node>> pair : state.facts) {
						ImmutableSet<Node> nodes = new ImmutableSet<Node>();

						for (Node node : pair.t1)
							nodes = nodes.replace(Replacer.replace(node, from, to));

						facts1 = facts1.put(pair.t0, nodes);
					}

					return new State(facts1);
				}
			});
		}

		return map(states, new Fun<State, State>() {
			public State apply(State state) {
				Prototype prototype = getPrototype(then);
				ImmutableSet<Node> facts = getFacts(state, prototype);
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

	private ImmutableSet<Node> getFacts(State state, Prototype prototype) {
		ImmutableSet<Node> results = state.facts.get(prototype);
		return results != null ? results : new ImmutableSet<Node>();
	}

	private State setFacts(State state, Prototype prototype, ImmutableSet<Node> nodes) {
		ImmutableMap<Prototype, ImmutableSet<Node>> facts = state.facts;
		return new State(nodes.iterator().hasNext() ? facts.replace(prototype, nodes) : facts.remove(prototype));
	}

	private Prototype getPrototype(Node node) {
		return Prototype.get(node);
	}

}
