package suite.lp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.ListMultimap;
import suite.immutable.IList;
import suite.lp.doer.Binder;
import suite.lp.doer.Configuration.ProverConfig;
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
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class ProveInterpreter {

	private SystemPredicates systemPredicates;

	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Trampoline[]> trampolinesByPrototype = new HashMap<>();

	private int nCutPoints;

	private Trampoline okay = rt -> {
		throw new RuntimeException("Impossibly okay");
	};
	private Trampoline fail = rt -> {
		throw new RuntimeException("Impossibly fail");
	};

	public interface Trampoline {
		public Trampoline prove(Runtime rt);
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
		private IList<Trampoline> rems = IList.end(); // Continuations
		private IList<Trampoline> alts = IList.end(); // Alternative
		private Journal journal = new Journal();
		private IList<Trampoline> cutPoints[];
		private Node query;
		private Prover prover;

		private Runtime(ProverConfig pc, Env ge, Trampoline tr) {
			this.ge = ge;
			pushAlt(tr);
			prover = new Prover(pc, null, journal);

			@SuppressWarnings("unchecked")
			IList<Trampoline>[] trampolineStacks = (IList<Trampoline>[]) new IList<?>[nCutPoints];
			cutPoints = trampolineStacks;
		}

		private void pushRem(Trampoline tr) {
			rems = IList.cons(tr, rems);
		}

		private void pushAlt(Trampoline tr) {
			alts = IList.cons(tr, alts);
		}
	}

	public ProveInterpreter(RuleSet rs) {
		systemPredicates = new SystemPredicates(null);

		for (Rule rule : rs.getRules())
			rules.put(Prototype.of(rule), rule);

		if (!rules.containsKey(null))
			compileAll();
		else
			throw new RuntimeException("Must not contain wild rules");
	}

	public Fun<ProverConfig, Boolean> compile(Node node) {
		return pc -> {
			boolean result[] = new boolean[] { false };
			run(pc, node, env -> result[0] = true);
			return result[0];
		};
	}

	private void run(ProverConfig pc, Node node, Sink<Env> sink) {
		Generalizer g1 = new Generalizer();
		CompileTime ct = new CompileTime(g1, nCutPoints++);
		Env env = g1.env();

		Trampoline sinker = rt_ -> {
			sink.sink(env);
			return fail;
		};

		Trampoline t = and(cutBegin(ct.cutIndex, newEnv(g1, compile0(ct, node))), sinker);
		trampoline(new Runtime(pc, env, t));
	}

	private void trampoline(Runtime rt) {
		while (!rt.alts.isEmpty()) {
			rt.pushRem(rt.alts.getHead());
			rt.alts = rt.alts.getTail();

			Trampoline rem;
			while ((rem = rt.rems.getHead()) != fail) {
				rt.rems = rt.rems.getTail();
				if (rem != okay)
					rt.pushRem(rem.prove(rt));
			}
		}

		rt.journal.undoAllBinds();
	}

	private void compileAll() {
		for (Pair<Prototype, Collection<Rule>> entry : rules.listEntries()) {
			List<Rule> rs = new ArrayList<>(entry.t1);
			int cutIndex = nCutPoints++;
			Trampoline tr = fail;

			for (int i = rs.size() - 1; i >= 0; i--) {
				Rule rule = rs.get(i);
				Node ruleHead = rule.getHead();
				Node ruleTail = rule.getTail();
				Generalizer g = new Generalizer();
				CompileTime ct = new CompileTime(g, cutIndex);

				Fun<Generalizer.Env, Node> f = ct.generalizer.compile(ruleHead);
				Trampoline tr0 = rt -> Binder.bind(rt.query, f.apply(rt.ge), rt.journal) ? okay : fail;
				Trampoline tr1 = compile0(ct, ruleTail);
				tr = or(newEnv(g, and(tr0, tr1)), tr);
			}

			getTrampolineByPrototype(entry.t0)[0] = cutBegin(cutIndex, tr);
		}
	}

	private Trampoline compile0(CompileTime ct, Node node) {
		Trampoline tr = null;
		Tree tree = Tree.decompose(node);

		if (tree != null) {
			Operator operator = tree.getOperator();
			Node lhs = tree.getLeft();
			Node rhs = tree.getRight();

			if (operator == TermOp.AND___) { // a, b
				Trampoline tr0 = compile0(ct, lhs);
				Trampoline tr1 = compile0(ct, rhs);
				tr = and(tr0, tr1);
			} else if (operator == TermOp.EQUAL_) { // a = b
				Fun<Generalizer.Env, Node> f0 = ct.generalizer.compile(lhs);
				Fun<Generalizer.Env, Node> f1 = ct.generalizer.compile(rhs);
				tr = rt -> Binder.bind(f0.apply(rt.ge), f1.apply(rt.ge), rt.journal) ? okay : fail;
			} else if (operator == TermOp.OR____) { // a; b
				Trampoline tr0 = compile0(ct, lhs);
				Trampoline tr1 = compile0(ct, rhs);
				tr = or(tr0, tr1);
			} else if (operator == TermOp.TUPLE_ && lhs instanceof Atom) // a b
				tr = callSystemPredicate(ct, ((Atom) lhs).getName(), rhs);
			else
				tr = callSystemPredicate(ct, operator.getName(), node);
		} else if (node instanceof Atom) {
			String name = ((Atom) node).getName();

			if (Util.stringEquals(name, Generalizer.cutName)) {
				int cutIndex = ct.cutIndex;
				tr = rt -> {
					rt.alts = rt.cutPoints[cutIndex];
					return okay;
				};
			} else if (Util.stringEquals(name, "fail"))
				tr = fail;
			else if (Util.stringEquals(name, "") || Util.stringEquals(name, "yes"))
				tr = okay;
			else
				tr = callSystemPredicate(ct, name, Atom.NIL);
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).getData();
			if (data instanceof Source<?>)
				tr = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
		}

		if (tr == null) {
			Prototype prototype = Prototype.of(node);
			if (rules.containsKey(prototype)) {
				Fun<Generalizer.Env, Node> f = ct.generalizer.compile(node);
				Trampoline trs[] = getTrampolineByPrototype(prototype);
				tr = rt -> {
					rt.query = f.apply(rt.ge);
					return trs[0]::prove;
				};
			}
		}

		if (tr != null)
			return tr;
		else
			throw new RuntimeException("Cannot understand " + node);
	}

	private Trampoline cutBegin(int cutIndex, Trampoline tr) {
		return rt -> {
			IList<Trampoline> alts0 = rt.cutPoints[cutIndex];
			rt.pushAlt(rt_ -> {
				rt_.cutPoints[cutIndex] = alts0;
				return fail;
			});

			rt.cutPoints[cutIndex] = rt.alts;
			return tr;
		};
	}

	private Trampoline newEnv(Generalizer g, Trampoline tr) {
		return rt -> {
			Env ge0 = rt.ge;
			rt.pushAlt(rt_ -> {
				rt_.ge = ge0;
				return fail;
			});

			rt.ge = g.env();
			return tr;
		};
	}

	private Trampoline and(Trampoline tr0, Trampoline tr1) {
		return rt -> {
			rt.pushRem(tr1);
			return tr0;
		};
	}

	private Trampoline or(Trampoline tr0, Trampoline tr1) {
		return rt -> {
			IList<Trampoline> rems0 = rt.rems;
			int pit = rt.journal.getPointInTime();
			rt.pushAlt(rt_ -> {
				rt_.journal.undoBinds(pit);
				rt_.rems = rems0;
				return tr1;
			});
			return tr0;
		};
	}

	private Trampoline callSystemPredicate(CompileTime ct, String name, Node pass) {
		Trampoline tr;

		if (Util.stringEquals(name, "call")) {
			Fun<Generalizer.Env, Node> f = ct.generalizer.compile(pass);
			tr = rt -> rt.prover.prove(f.apply(rt.ge)) ? okay : fail;
		} else if (Util.stringEquals(name, "once")) {
			Trampoline tr1 = compile0(ct, pass);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return okay;
				});
				return tr1;
			};
		} else if (Util.stringEquals(name, "not")) {
			Trampoline tr1 = compile0(ct, pass);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				IList<Trampoline> rems0 = rt.rems;
				int pit = rt.journal.getPointInTime();
				rt.pushRem(rt_ -> {
					rt_.journal.undoBinds(pit);
					rt_.alts = alts0;
					return fail;
				});
				rt.pushAlt(rt_ -> {
					rt_.rems = rems0;
					return okay;
				});
				return tr1;
			};
		} else {
			SystemPredicate systemPredicate = systemPredicates.get(name);
			if (systemPredicate != null) {
				Fun<Generalizer.Env, Node> f = ct.generalizer.compile(pass);
				tr = rt -> systemPredicate.prove(rt.prover, f.apply(rt.ge)) ? okay : fail;
			} else
				tr = null;
		}

		return tr;
	}

	private Trampoline[] getTrampolineByPrototype(Prototype prototype) {
		return trampolinesByPrototype.computeIfAbsent(prototype, k -> new Trampoline[1]);
	}

}
