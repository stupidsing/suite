package suite.lp.sewing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.ListMultimap;
import suite.immutable.IList;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.lp.predicate.SystemPredicates;
import suite.lp.sewing.SewingGeneralizer.Env;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.Pair;
import suite.util.Util;

/**
 * Compile logical rules into lambda-sews and run them. Supposed to be faster
 * but no improvement generally. No actual measurement was conducted.
 *
 * Would break under following conditions:
 * 
 * - rules containing wild searches that are unable to derive prototype from;
 * 
 * - asserts or retracts.
 *
 * @author ywsing
 */
public class SewingProver {

	private SystemPredicates systemPredicates;

	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Trampoline[]> trampolinesByPrototype = new HashMap<>();

	private Trampoline okay = rt -> {
		throw new RuntimeException("Impossibly okay");
	};
	private Trampoline fail = rt -> {
		throw new RuntimeException("Impossibly fail");
	};

	public interface Trampoline {
		public Trampoline prove(Runtime rt);
	}

	private class Runtime {
		private Env ge;
		private IList<Trampoline> cutPoint;
		private Node query;
		private Journal journal = new Journal();
		private IList<Trampoline> rems = IList.end(); // Continuations
		private IList<Trampoline> alts = IList.end(); // Alternative
		private Prover prover;

		private Runtime(ProverConfig pc, Env ge, Trampoline tr) {
			this.ge = ge;
			pushAlt(tr);
			prover = new Prover(pc, null, journal);
		}

		private void post(Runnable r) {
			pushRem(rt -> {
				r.run();
				return okay;
			});
			pushAlt(rt -> {
				r.run();
				return fail;
			});
		}

		private void pushRem(Trampoline tr) {
			if (tr != okay)
				rems = IList.cons(tr, rems);
		}

		private void pushAlt(Trampoline tr) {
			alts = IList.cons(tr, alts);
		}
	}

	public SewingProver(RuleSet rs) {
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
		SewingGeneralizer sg = new SewingGeneralizer();
		Env env = sg.env();

		Trampoline sinker = rt_ -> {
			sink.sink(env);
			return fail;
		};

		Trampoline t = and(cutBegin(newEnv(sg, compile0(sg, node))), sinker);
		trampoline(new Runtime(pc, env, t));
	}

	private void trampoline(Runtime rt) {
		while (!rt.alts.isEmpty()) {
			rt.rems = IList.cons(rt.alts.head, IList.end());
			rt.alts = rt.alts.tail;

			Trampoline rem;
			while ((rem = rt.rems.head) != fail) {
				rt.rems = rt.rems.tail;
				rt.pushRem(rem.prove(rt));
			}
		}

		rt.journal.undoAllBinds();
	}

	private void compileAll() {
		for (Pair<Prototype, Collection<Rule>> entry : rules.listEntries()) {
			List<Rule> rules = new ArrayList<>(entry.t1);
			Trampoline tr0 = compileRules(rules);
			Trampoline tr;

			// Second-level indexing optimization
			if (rules.size() >= 6) {
				Map<Prototype, List<Rule>> rulesByProto1 = Read.from(rules) //
						.groupBy(rule -> Prototype.of(rule, 1)) //
						.collect(As.map());

				if (!rulesByProto1.containsKey(null)) {
					Map<Prototype, Trampoline> trByProto1 = Read.from(rulesByProto1) //
							.map(kv -> Pair.of(kv.t0, compileRules(kv.t1))) //
							.collect(As.map());

					tr = rt -> {
						Prototype proto = Prototype.of(rt.query, 1);
						if (proto != null) {
							Trampoline tr_ = trByProto1.get(proto);
							return tr_ != null ? tr_ : fail;
						} else
							return tr0;
					};
				} else
					tr = tr0;
			} else
				tr = tr0;

			getTrampolineByPrototype(entry.t0)[0] = tr;
		}
	}

	private Trampoline compileRules(List<Rule> rules) {
		Trampoline tr = fail;

		for (int i = rules.size() - 1; i >= 0; i--) {
			Rule rule = rules.get(i);
			SewingGeneralizer sg = new SewingGeneralizer();
			Fun<Env, Node> f = sg.compile(rule.head);

			Trampoline tr0 = rt -> Binder.bind(rt.query, f.apply(rt.ge), rt.journal) ? okay : fail;
			Trampoline tr1 = compile0(sg, rule.tail);
			tr = or(newEnv(sg, and(tr0, tr1)), tr);
		}

		tr = saveEnv(cutBegin(tr));
		tr = Suite.isProverTrace ? log(tr) : tr;
		return tr;
	}

	private Trampoline compile0(SewingGeneralizer sg, Node node) {
		Trampoline tr = null;
		node = node.finalNode();
		List<Node> list;
		Tree tree;
		Node m[];

		if ((list = breakdown(TermOp.AND___, node)).size() > 1) {
			List<Trampoline> trs = Read.from(list).map(n -> compile0(sg, n)).reverse().toList();
			tr = rt -> {
				for (Trampoline tr_ : trs)
					rt.pushRem(tr_);
				return okay;
			};
		} else if ((m = Suite.match(".0; .1", node)) != null) {
			Trampoline tr0 = compile0(sg, m[0]);
			Trampoline tr1 = compile0(sg, m[1]);
			tr = or(tr0, tr1);
		} else if ((m = Suite.match(".0 = .1", node)) != null) {
			Fun<Env, Node> f0 = sg.compile(m[0]);
			Fun<Env, Node> f1 = sg.compile(m[1]);
			tr = rt -> Binder.bind(f0.apply(rt.ge), f1.apply(rt.ge), rt.journal) ? okay : fail;
		} else if ((m = Suite.match(".0 .1", node)) != null && m[0] instanceof Atom)
			tr = callSystemPredicate(sg, ((Atom) m[0]).name, m[1]);
		else if ((m = Suite.match("if .0 then .1 else .2", node)) != null) {
			Trampoline tr0 = compile0(sg, m[0]);
			Trampoline tr1 = compile0(sg, m[1]);
			Trampoline tr2 = compile0(sg, m[2]);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				int pit = rt.journal.getPointInTime();
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return tr1;
				});
				rt.pushAlt(rt_ -> {
					rt_.journal.undoBinds(pit);
					return tr2;
				});
				return tr0;
			};
		} else if ((tree = Tree.decompose(node)) != null)
			tr = callSystemPredicate(sg, tree.getOperator().getName(), node);
		else if (node instanceof Atom) {
			String name = ((Atom) node).name;
			if (Util.stringEquals(name, SewingGeneralizer.cutName))
				tr = cutEnd();
			else if (Util.stringEquals(name, ""))
				tr = okay;
			else if (Util.stringEquals(name, "fail"))
				tr = fail;
			else if (name.startsWith(SewingGeneralizer.variablePrefix)) {
				Fun<Env, Node> f = sg.compile(node);
				tr = rt -> rt.prover.prove(f.apply(rt.ge)) ? okay : fail;
			} else
				tr = callSystemPredicate(sg, name, Atom.NIL);
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).data;
			if (data instanceof Source<?>)
				tr = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
		}

		if (tr == null) {
			Prototype prototype = Prototype.of(node);
			if (rules.containsKey(prototype)) {
				Fun<Env, Node> f = sg.compile(node);
				Trampoline trs[] = getTrampolineByPrototype(prototype);
				tr = rt -> {
					Node query0 = rt.query;
					rt.query = f.apply(rt.ge);
					rt.pushAlt(rt_ -> {
						rt_.query = query0;
						return fail;
					});
					return trs[0]::prove;
				};
			}
		}

		if (tr != null)
			return tr;
		else
			throw new RuntimeException("Cannot understand " + node);
	}

	private Trampoline callSystemPredicate(SewingGeneralizer sg, String name, Node pass) {
		Trampoline tr;

		if (Util.stringEquals(name, "not")) {
			Trampoline tr1 = compile0(sg, pass);
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
		} else if (Util.stringEquals(name, "once")) {
			Trampoline tr1 = compile0(sg, pass);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return okay;
				});
				return tr1;
			};
		} else {
			SystemPredicate systemPredicate = systemPredicates.get(name);
			if (systemPredicate != null) {
				Fun<Env, Node> f = sg.compile(pass);
				tr = rt -> systemPredicate.prove(rt.prover, f.apply(rt.ge)) ? okay : fail;
			} else
				tr = null;
		}

		return tr;
	}

	private Trampoline cutBegin(Trampoline tr) {
		return rt -> {
			IList<Trampoline> cutPoint0 = rt.cutPoint;
			rt.post(() -> rt.cutPoint = cutPoint0);
			rt.cutPoint = rt.alts;
			return tr;
		};
	}

	private Trampoline cutEnd() {
		return rt -> {
			rt.alts = rt.cutPoint;
			return okay;
		};
	}

	private Trampoline log(Trampoline tr) {
		return rt -> {
			String m = Formatter.dump(rt.query);
			LogUtil.info("QUERY " + m);
			rt.pushRem(rt_ -> {
				LogUtil.info("OK___ " + m);
				return okay;
			});
			rt.pushAlt(rt_ -> {
				LogUtil.info("FAIL_ " + m);
				return fail;
			});
			return tr;
		};
	}

	private Trampoline saveEnv(Trampoline tr) {
		return rt -> {
			Env ge0 = rt.ge;
			rt.post(() -> rt.ge = ge0);
			return tr;
		};
	}

	private Trampoline newEnv(SewingGeneralizer sg, Trampoline tr) {
		return rt -> {
			rt.ge = sg.env();
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

	private List<Node> breakdown(Operator operator, Node node) {
		ArrayList<Node> list = new ArrayList<>();
		Tree tree;
		while ((tree = Tree.decompose(node, operator)) != null) {
			list.add(tree.getLeft());
			node = tree.getRight();
		}
		list.add(node);
		return list;
	}

	private Trampoline[] getTrampolineByPrototype(Prototype prototype) {
		return trampolinesByPrototype.computeIfAbsent(prototype, k -> new Trampoline[1]);
	}

}
