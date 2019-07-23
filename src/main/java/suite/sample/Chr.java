package suite.sample;

import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Rewrite;
import suite.persistent.PerMap;
import suite.persistent.PerSet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.To;

/**
 * Constraint handling rules implementation.
 *
 * @author ywsing
 */
public class Chr {

	private Rewrite rw = new Rewrite();

	private List<Rule> rules = new ArrayList<>();
	private Prover prover = new Prover(Suite.newRuleSet());

	private class Rule {
		private List<Node> givens = new ArrayList<>();
		private List<Node> ifs = new ArrayList<>();
		private List<Node> thens = new ArrayList<>();
		private Node when = Atom.NIL;
	}

	private class State {
		private PerMap<Prototype, PerSet<Node>> factsByPrototype;

		public State(PerMap<Prototype, PerSet<Node>> factsByPrototype) {
			this.factsByPrototype = factsByPrototype;
		}
	}

	public void addRule(Node node) {
		var rule = new Rule();

		while (node != Atom.of("end")) {
			var t0 = Tree.decompose(node, TermOp.TUPLE_);
			Tree t1 = t0 != null ? Tree.decompose(t0.getRight(), TermOp.TUPLE_) : null;

			if (t1 != null) {
				var key = t0.getLeft();
				var value = t1.getLeft();
				node = t1.getRight();

				if (key == Atom.of("given"))
					rule.givens = To.list(Tree.read(value));
				else if (key == Atom.of("if"))
					rule.ifs = To.list(Tree.read(value));
				else if (key == Atom.of("then"))
					rule.thens = To.list(Tree.read(value));
				else if (key == Atom.of("when"))
					rule.when = value;
				else
					fail("invalid key " + key);
			} else
				fail("invalid rule " + node);
		}

		rules.add(rule);
	}

	public Collection<Node> chr(Collection<Node> facts) {
		var state = new State(PerMap.empty());

		for (var fact : facts) {
			var prototype = Prototype.of(fact);
			state = setFacts(state, prototype, getFacts(state, prototype).replace(fact));
		}

		State state1;

		while ((state1 = chr(state)) != null)
			state = state1;

		var nodes1 = new ArrayList<Node>();

		for (var e : state.factsByPrototype)
			nodes1.addAll(To.list(e.v));

		return nodes1;
	}

	private State chr(State state) {
		return Read.from(rules).concatMap(rule -> chr(state, rule)).first();
	}

	private Streamlet<State> chr(State state, Rule rule) {
		var generalizer = new Generalizer();
		var trail = new Trail();
		var states = Read.each(state);

		for (var if_ : rule.ifs)
			states = chrIf(states, trail, generalizer.generalize(if_));

		for (var given : rule.givens)
			states = chrGiven(states, trail, generalizer.generalize(given));

		states = chrWhen(states, generalizer.generalize(rule.when));

		for (var then : rule.thens)
			states = chrThen(states, generalizer.generalize(then));

		return states;
	}

	private Streamlet<State> chrIf(Streamlet<State> states, Trail trail, Node if_) {
		var prototype = Prototype.of(if_);

		Fun<State, Streamlet<State>> fun = state -> {
			var facts = getFacts(state, prototype);
			Predicate<Node> bindFun = bindFun(trail, if_);
			return facts.streamlet().filter(bindFun).map(node -> setFacts(state, prototype, facts.remove(node)));
		};

		return states.concatMap(fun);
	}

	private Streamlet<State> chrGiven(Streamlet<State> states, Trail trail, Node given) {
		var prototype = Prototype.of(given);

		return states.filter(state -> {
			var facts = getFacts(state, prototype);
			Predicate<Node> bindFun = bindFun(trail, given);
			return facts.streamlet().isAny(bindFun);
		});
	}

	private Streamlet<State> chrThen(Streamlet<State> states, Node then) {
		var generalizer = new Generalizer();
		Atom a = atom(".a"), b = atom(".b");

		if (Binder.bind(then, generalizer.generalize(Suite.substitute(".0 = .1", a, b)), new Trail())) {

			// built-in syntactic equality
			var from = generalizer.getVariable(a);
			var to = generalizer.getVariable(b);

			states = states.map(new Fun<>() {
				public State apply(State state) {
					var factsByPrototype1 = PerMap.<Prototype, PerSet<Node>> empty();
					for (var e : state.factsByPrototype)
						factsByPrototype1 = factsByPrototype1.put(e.k, replace(e.v));
					return new State(factsByPrototype1);
				}

				private PerSet<Node> replace(PerSet<Node> facts) {
					var facts1 = PerSet.<Node> empty();
					for (var node : facts)
						facts1 = facts1.replace(rw.replace(from, to, node));
					return facts1;
				}
			});
		}

		return states.map(state -> {
			var prototype = Prototype.of(then);
			var facts = getFacts(state, prototype);
			return setFacts(state, prototype, facts.replace(then));
		});
	}

	private Streamlet<State> chrWhen(Streamlet<State> states, Node when) {
		return states.filter(state -> prover.prove(when));
	}

	private Predicate<Node> bindFun(Trail trail, Node node0) {
		var pit = trail.getPointInTime();

		return node1 -> {
			trail.unwind(pit);
			return Binder.bind(node0, node1, trail);
		};
	}

	private Atom atom(String name) {
		return Atom.of(name);
	}

	private PerSet<Node> getFacts(State state, Prototype prototype) {
		var results = state.factsByPrototype.get(prototype);
		return results != null ? results : PerSet.empty();
	}

	private State setFacts(State state, Prototype prototype, PerSet<Node> nodes) {
		var facts = state.factsByPrototype;
		return new State(nodes.streamlet().first() != null ? facts.replace(prototype, nodes) : facts.remove(prototype));
	}

}
