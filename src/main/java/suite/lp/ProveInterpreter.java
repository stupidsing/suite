package suite.lp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Generalizer.Env;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.lp.predicate.SystemPredicates;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class ProveInterpreter {

	private Prover prover;
	private SystemPredicates systemPredicates;

	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Cps> cpsByPrototype;

	// Continuation passing style invocation
	public interface Cps {
		public void prove(Runtime rt, Runnable continuation);
	}

	private class CompileTime {
		private Generalizer generalizer;

		public CompileTime(Generalizer generalizer) {
			this.generalizer = generalizer;
		}
	}

	private class Runtime {
		private Generalizer.Env ge;
		private Journal journal;

		private Runtime(Runtime rt, Generalizer.Env ge1) {
			this(ge1, rt.journal);
		}

		public Runtime(Env ge, Journal journal) {
			this.ge = ge;
			this.journal = journal;
		}
	}

	public ProveInterpreter(RuleSet rs) {
		prover = new Prover(rs);
		systemPredicates = new SystemPredicates(prover);

		for (Rule rule : rs.getRules())
			rules.put(Prototype.of(rule), rule);
	}

	public Source<Boolean> compile(Node node) {
		cpsByPrototype = new HashMap<>();

		for (Pair<Prototype, Collection<Rule>> entry : rules.listEntries()) {
			Node query = new Reference();
			Cps cps = (rt, cont) -> {
			};

			List<Rule> rs = new ArrayList<>(entry.t1);

			for (int i = rs.size() - 1; i >= 0; i--) {
				Rule rule = rs.get(i);

				Node rn = Tree.of(TermOp.AND___ //
						, Tree.of(TermOp.EQUAL_ //
								, query //
								, rule.getHead()) //
						, rule.getTail());

				Generalizer g = new Generalizer();

				Cps cps0 = compile0(new CompileTime(g), rn);
				Cps cps1 = (rt, cont) -> cps0.prove(new Runtime(rt, g.env()), cont);
				cps = or(cps1, cps);
			}

			cpsByPrototype.put(entry.t0, cps);
		}

		Generalizer g1 = new Generalizer();
		Cps cps_ = compile0(new CompileTime(g1), node);

		return () -> {
			boolean result[] = new boolean[] { false };
			cps_.prove(new Runtime(g1.env(), new Journal()), () -> {
				result[0] = true;
			});

			return result[0];
		};
	}

	private Cps compile0(CompileTime ct, Node node) {
		Cps result = null;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.AND___) { // a, b
				Cps cps0 = compile0(ct, lhs);
				Cps cps1 = compile0(ct, rhs);
				result = (rt, cont) -> cps0.prove(rt, () -> cps1.prove(rt, cont));
			} else if (operator == TermOp.EQUAL_) { // a = b
				Fun<Generalizer.Env, Node> f0 = ct.generalizer.compile(lhs);
				Fun<Generalizer.Env, Node> f1 = ct.generalizer.compile(rhs);
				result = (rt, cont) -> {
					if (Binder.bind(f0.apply(rt.ge), f1.apply(rt.ge), rt.journal))
						cont.run();
				};
			} else if (operator == TermOp.OR____) { // a; b
				Cps cps0 = compile0(ct, lhs);
				Cps cps1 = compile0(ct, rhs);
				result = or(cps0, cps1);
			} else if (operator == TermOp.TUPLE_ && lhs instanceof Atom) // a b
				result = callSystemPredicate(ct, ((Atom) lhs).getName(), rhs);
			else
				result = callSystemPredicate(ct, operator.getName(), node);
		} else if (node instanceof Atom) {
			String name = ((Atom) node).getName();

			if (Util.stringEquals(name, "fail"))
				result = (rt, cont) -> {
				};
			else if (Util.stringEquals(name, "") || Util.stringEquals(name, "yes"))
				result = (rt, cont) -> {
					cont.run();
				};
			else
				result = callSystemPredicate(ct, name, Atom.NIL);
		}

		if (result == null) {
			Cps cps = cpsByPrototype.get(Prototype.of(node));
			if (cps != null)
				result = cps::prove;
		}

		return result;
	}

	private Cps or(Cps cps0, Cps cps1) {
		return (rt, cont) -> {
			int pit = rt.journal.getPointInTime();
			cps0.prove(rt, cont);
			rt.journal.undoBinds(pit);
			cps1.prove(rt, cont);
		};
	}

	private Cps callSystemPredicate(CompileTime ct, String name, Node pass) {
		SystemPredicate systemPredicate = systemPredicates.get(name);
		if (systemPredicate != null) {
			Fun<Generalizer.Env, Node> f = ct.generalizer.compile(pass);
			return (rt, cont) -> {
				if (systemPredicate.prove(prover, f.apply(rt.ge)))
					cont.run();
			};
		} else
			return null;
	}

}
