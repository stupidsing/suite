package suite.chr;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.immutable.ImmutableMap;
import suite.immutable.ImmutableSet;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Node;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

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
		private ImmutableMap<Prototype, ImmutableSet<Node>> facts = new ImmutableMap<>();

		public State(ImmutableMap<Prototype, ImmutableSet<Node>> facts) {
			this.facts = facts;
		}
	}

	public State chr(final State state) {
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
				final ImmutableSet<Node> facts = state.facts.get(prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, if_);
				Source<Node> bindedIfs = filter(FunUtil.asSource(facts), bindFun);

				return map(bindedIfs, new Fun<Node, State>() {
					public State apply(Node node) {
						return new State(state.facts.replace(prototype, facts.remove(node)));
					}
				});
			}
		}));
	}

	private Source<State> chrGiven(Source<State> states, final Journal journal, final Node given) {
		final Prototype prototype = getPrototype(given);

		return FunUtil.concat(map(states, new Fun<State, Source<State>>() {
			public Source<State> apply(final State state) {
				ImmutableSet<Node> facts = state.facts.get(prototype);
				Fun<Node, Boolean> bindFun = bindFun(journal, given);
				boolean isMatch = or(map(FunUtil.asSource(facts), bindFun));
				return isMatch ? FunUtil.asSource(state) : FunUtil.<State> nullSource();
			}
		}));
	}

	private Source<State> chrThen(Source<State> states, final Node then) {
		final Prototype prototype = getPrototype(then);

		return map(states, new Fun<State, State>() {
			public State apply(State state) {
				ImmutableSet<Node> facts = state.facts.get(prototype);
				return new State(state.facts.replace(prototype, facts.add(then)));
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

	private Prototype getPrototype(Node node) {
		return Prototype.get(node);
	}

}
