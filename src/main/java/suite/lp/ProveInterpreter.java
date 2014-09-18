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
import suite.node.Data;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class ProveInterpreter {

	private Prover prover;
	private SystemPredicates systemPredicates;

	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Cps> cpsByPrototype;

	private int nCutPoints;

	private Runnable runVoid = () -> {
	};

	// Continuation passing style invocation
	public interface Cps {
		public void prove(Runtime rt, Runnable continuation);
	}

	private class CompileTime {
		private Generalizer generalizer;
		private int cutIndex;

		public CompileTime(Generalizer generalizer, int cutIndex) {
			this.generalizer = generalizer;
			this.cutIndex = cutIndex;
		}
	}

	private class Runtime {
		private Env ge;
		private Journal journal;
		private Runnable alternative;
		private Runnable cutPoints[];

		private Runtime(Runtime rt, Env ge1) {
			this(ge1, rt.journal, rt.alternative, rt.cutPoints);
		}

		private Runtime(Env ge) {
			this(ge, new Journal(), runVoid, new Runnable[nCutPoints]);
		}

		private Runtime(Env ge, Journal journal, Runnable alternative, Runnable cutPoints[]) {
			this.ge = ge;
			this.journal = journal;
			this.alternative = alternative;
			this.cutPoints = cutPoints;
		}
	}

	public ProveInterpreter(RuleSet rs) {
		prover = new Prover(rs);
		systemPredicates = new SystemPredicates(prover);

		for (Rule rule : rs.getRules())
			rules.put(Prototype.of(rule), rule);
	}

	public Source<Boolean> compile(Node node) {
		return () -> {
			boolean result[] = new boolean[] { false };
			run(node, env -> result[0] = true);
			return result[0];
		};
	}

	private void run(Node node, Sink<Env> sink) {
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

				CompileTime ct = new CompileTime(g, nCutPoints++);
				Cps cps1 = cutBegin(ct, compile0(ct, rn));
				cps = or(cps1, cps);
			}

			cpsByPrototype.put(entry.t0, cps);
		}

		Generalizer g1 = new Generalizer();
		CompileTime ct = new CompileTime(g1, nCutPoints++);
		Cps cps_ = cutBegin(ct, compile0(ct, node));

		Env env = g1.env();
		Runtime rt = new Runtime(env);
		Runnable runnable = () -> cps_.prove(rt, () -> sink.sink(env));

		while (runnable != runVoid) {
			runnable.run();
			runnable = rt.alternative;
		}

		rt.journal.undoAllBinds();
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

			if (Util.stringEquals(name, Generalizer.cutName)) {
				int cutIndex = ct.cutIndex;
				result = (rt, cont) -> rt.alternative = rt.cutPoints[cutIndex];
			} else if (Util.stringEquals(name, "fail"))
				result = (rt, cont) -> {
				};
			else if (Util.stringEquals(name, "") || Util.stringEquals(name, "yes"))
				result = (rt, cont) -> cont.run();
			else
				result = callSystemPredicate(ct, name, Atom.NIL);
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).getData();
			if (data instanceof Source<?>)
				result = (rt, cont) -> {
					if (((Source<?>) data).source() == Boolean.TRUE)
						cont.run();
				};
		}

		if (result == null) {
			Cps cps = cpsByPrototype.get(Prototype.of(node));
			if (cps != null)
				result = cps::prove;
		}

		if (result != null)
			return result;
		else
			throw new RuntimeException("Cannot understand " + node);
	}

	private Cps cutBegin(CompileTime ct, Cps cps0) {
		Generalizer g = ct.generalizer;
		Cps cps1 = newEnvironment(g, cps0);

		int cutIndex = ct.cutIndex;
		return (rt, cont) -> {
			Runnable alt0 = rt.cutPoints[cutIndex];
			rt.cutPoints[cutIndex] = rt.alternative;
			cps1.prove(rt, cont);
			rt.cutPoints[cutIndex] = alt0;
		};
	}

	private Cps newEnvironment(Generalizer g, Cps cps) {
		return (rt, cont) -> {
			Env ge0 = rt.ge;
			rt.ge = g.env();
			cps.prove(rt, cont);
			rt.ge = ge0;
		};
	}

	private Cps or(Cps cps0, Cps cps1) {
		return (rt, cont) -> {
			Runnable alternative0 = rt.alternative;
			int pit = rt.journal.getPointInTime();

			rt.alternative = () -> {
				rt.journal.undoBinds(pit);
				rt.alternative = alternative0;
				cps1.prove(rt, cont);
			};

			cps0.prove(rt, cont);
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
