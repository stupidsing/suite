package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import suite.Suite;
import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.immutable.IList;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.doer.ProverConstant;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.lp.predicate.SystemPredicates;
import suite.lp.sewing.SewingBinder;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingProver;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingExpressionImpl.Evaluate;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.SuiteException;
import suite.node.util.TreeRewriter;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
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
public class SewingProverImpl implements SewingProver {

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
		private Env env;
		private IList<Trampoline> cutPoint;
		private Node query;
		private Journal journal = new Journal();
		private IList<Trampoline> rems = IList.end(); // Continuations
		private IList<Trampoline> alts = IList.end(); // Alternatives
		private Prover prover;
		private String indent = "";
		private Sink<Node> handler = node -> {
			throw new SuiteException(node);
		};

		private Runtime(ProverConfig pc, Trampoline tr) {
			pushAlt(tr);
			prover = new Prover(pc, null, journal);
		}

		private BindEnv bindEnv() {
			return new BindEnv(journal, env);
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

	public SewingProverImpl(RuleSet rs) {
		systemPredicates = new SystemPredicates(null);
		rules = Read.from(rs.getRules()).groupBy(Prototype::of).collect(As.multimap());

		if (!rules.containsKey(null))
			compileAll();
		else
			throw new RuntimeException("Must not contain wild rules");
	}

	public Fun<ProverConfig, Boolean> compile(Node node) {
		SewingBinder sb = new SewingBinderImpl();
		Trampoline tr = cutBegin(newEnv(sb, compile0(sb, new Generalizer().generalize(node))));

		return pc -> {
			boolean result[] = new boolean[] { false };

			trampoline(new Runtime(pc, rt -> {
				rt.pushRem(rt_ -> {
					result[0] = true;
					return fail;
				});
				return tr;
			}));

			return result[0];
		};
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
			Prototype prototype = entry.t0;
			boolean isTrace = isTrace(prototype);

			List<Rule> rules = new ArrayList<>(entry.t1);
			Trampoline tr0 = compileRules(rules, isTrace);
			Trampoline tr;

			// Second-level indexing optimization
			if (rules.size() >= 6) {
				Map<Prototype, List<Rule>> rulesByProto1 = Read.from(rules) //
						.groupBy(rule -> Prototype.of(rule, 1)) //
						.collect(As.map());

				if (!rulesByProto1.containsKey(null)) {
					Map<Prototype, Trampoline> trByProto1 = Read.from(rulesByProto1) //
							.map(kv -> Pair.of(kv.t0, compileRules(kv.t1, isTrace))) //
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

			getTrampolineByPrototype(prototype)[0] = tr;
		}
	}

	private Trampoline compileRules(List<Rule> rules, boolean isTrace) {
		boolean hasCut = Read.from(rules) //
				.map(rule -> new TreeRewriter().contains(ProverConstant.cut, rule.tail)) //
				.fold(false, (b0, b1) -> b0 || b1);

		Streamlet<Trampoline> trs = Read.from(rules).map(rule -> {
			Generalizer generalizer = new Generalizer();
			Node head = generalizer.generalize(rule.head);
			Node tail = generalizer.generalize(rule.tail);

			SewingBinder sb = new SewingBinderImpl();
			BiPredicate<BindEnv, Node> p = sb.compileBind(head);
			Trampoline tr1 = compile0(sb, tail);
			return newEnv(sb, rt -> p.test(rt.bindEnv(), rt.query) ? tr1 : fail);
		});

		Trampoline tr0 = or(trs);
		Trampoline tr1 = hasCut ? cutBegin(tr0) : tr0;
		Trampoline tr2 = saveEnv(tr1);
		return isTrace ? log(tr2) : tr2;
	}

	private Trampoline compile0(SewingBinder sb, Node node) {
		Trampoline tr = null;
		node = node.finalNode();
		List<Node> list;
		Tree tree;
		Node m[];

		if ((list = breakdown(TermOp.AND___, node)).size() > 1)
			tr = and(Read.from(list).map(n -> compile0(sb, n)));
		else if ((list = breakdown(TermOp.OR____, node)).size() > 1)
			tr = or(Read.from(list).map(n -> compile0(sb, n)));
		else if ((m = Suite.matcher(".0 = .1").apply(node)) != null) {
			boolean b = complexity(m[0]) > complexity(m[1]);
			Node n0 = b ? m[0] : m[1];
			Node n1 = b ? m[1] : m[0];
			BiPredicate<BindEnv, Node> p = sb.compileBind(n0);
			Fun<Env, Node> f = sb.compile(n1);
			tr = rt -> p.test(rt.bindEnv(), f.apply(rt.env)) ? okay : fail;
		} else if ((m = Suite.matcher("builtin:.0:.1 .2").apply(node)) != null) {
			String className = ((Atom) m[0]).name;
			String fieldName = ((Atom) m[1]).name;
			BuiltinPredicate predicate;
			try {
				Class<?> clazz = Class.forName(className);
				predicate = (BuiltinPredicate) clazz.getField(fieldName).get(clazz.newInstance());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			tr = callPredicate(sb, predicate, m[2]);
		} else if ((m = Suite.matcher("if .0 .1 .2").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			Trampoline tr1 = compile0(sb, m[1]);
			Trampoline tr2 = compile0(sb, m[2]);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				IList<Trampoline> rems0 = rt.rems;
				int pit = rt.journal.getPointInTime();
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return tr1;
				});
				rt.pushAlt(rt_ -> {
					rt_.journal.undoBinds(pit);
					rt_.rems = rems0;
					return tr2;
				});
				return tr0;
			};
		} else if ((m = Suite.matcher("let .0 .1").apply(node)) != null) {
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[0]);
			Evaluate eval = new SewingExpressionImpl(sb).compile(m[1]);
			tr = rt -> p.test(rt.bindEnv(), Int.of(eval.evaluate(rt.env))) ? okay : fail;
		} else if ((m = Suite.matcher("not .0").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				IList<Trampoline> rems0 = rt.rems;
				int pit = rt.journal.getPointInTime();
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return fail;
				});
				rt.pushAlt(rt_ -> {
					rt_.journal.undoBinds(pit);
					rt_.rems = rems0;
					return okay;
				});
				return tr0;
			};
		} else if ((m = Suite.matcher("once .0").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return okay;
				});
				return tr0;
			};
		} else if ((m = Suite.matcher("throw .0").apply(node)) != null) {
			Fun<Env, Node> f = sb.compile(m[0]);
			tr = rt -> {
				rt.handler.sink(new Cloner().clone(f.apply(rt.env)));
				return okay;
			};
		} else if ((m = Suite.matcher("try .0 .1 .2").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, Suite.substitute("once .0", m[0]));
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[1]);
			Trampoline catch0 = compile0(sb, m[2]);
			tr = rt -> {
				BindEnv be = rt.bindEnv();
				int pit = rt.journal.getPointInTime();
				Sink<Node> handler0 = rt.handler;
				Env env0 = rt.env;
				IList<Trampoline> alts0 = rt.alts;
				IList<Trampoline> rems0 = rt.rems;
				rt.handler = node_ -> {
					rt.handler = handler0;
					rt.journal.undoBinds(pit);
					if (p.test(be, node_)) {
						rt.env = env0;
						rt.alts = alts0;
						rt.rems = rems0;
						rt.pushRem(catch0);
					} else
						handler0.sink(node_);
				};
				rt.post(() -> rt.handler = handler0);
				return tr0;
			};
		} else if ((m = Suite.matcher(".0 .1").apply(node)) != null && m[0] instanceof Atom)
			tr = callSystemPredicate(sb, ((Atom) m[0]).name, m[1]);
		else if ((tree = Tree.decompose(node)) != null)
			tr = callSystemPredicate(sb, tree.getOperator().getName(), node);
		else if (node instanceof Atom) {
			String name = ((Atom) node).name;
			if (node == ProverConstant.cut)
				tr = cutEnd();
			else if (Util.stringEquals(name, ""))
				tr = okay;
			else if (Util.stringEquals(name, "fail"))
				tr = fail;
			else
				tr = callSystemPredicate(sb, name, Atom.NIL);
		} else if (node instanceof Reference) {
			Fun<Env, Node> f = sb.compile(node);
			return rt -> compile0(new SewingBinder() {
				public BiPredicate<BindEnv, Node> compileBind(Node node) {
					return (be, n) -> Binder.bind(node, n, be.journal);
				}

				public Fun<Env, Node> compile(Node node) {
					return env -> node;
				}

				public Env env() {
					return new Env(new Reference[0]);
				}
			}, f.apply(rt.env));
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).data;
			if (data instanceof Source<?>)
				tr = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
		}

		if (tr == null) {
			Prototype prototype = Prototype.of(node);
			if (rules.containsKey(prototype)) {
				Fun<Env, Node> f = sb.compile(node);
				Trampoline trs[] = getTrampolineByPrototype(prototype);
				tr = rt -> {
					Node query0 = rt.query;
					rt.query = f.apply(rt.env);
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

	private Trampoline callSystemPredicate(SewingBinder sb, String name, Node pass) {
		BuiltinPredicate predicate = systemPredicates.get(name);
		return predicate != null ? callPredicate(sb, predicate, pass) : null;
	}

	private Trampoline callPredicate(SewingBinder sb, BuiltinPredicate predicate, Node pass) {
		Fun<Env, Node> f = sb.compile(pass);
		return rt -> predicate.prove(rt.prover, f.apply(rt.env)) ? okay : fail;
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
			String indent0 = rt.indent;
			rt.indent = indent0 + "| ";
			String m = Formatter.dump(rt.query);

			LogUtil.info(indent0 + "QUERY " + m);
			rt.pushRem(rt_ -> {
				rt.indent = indent0;
				LogUtil.info(indent0 + "OK___ " + m);
				return okay;
			});
			rt.pushAlt(rt_ -> {
				rt.indent = indent0;
				LogUtil.info(indent0 + "FAIL_ " + m);
				return fail;
			});
			return tr;
		};
	}

	private Trampoline saveEnv(Trampoline tr) {
		return rt -> {
			Env env0 = rt.env;
			rt.post(() -> rt.env = env0);
			return tr;
		};
	}

	private Trampoline newEnv(SewingBinder sb, Trampoline tr) {
		return rt -> {
			rt.env = sb.env();
			return tr;
		};
	}

	private Trampoline and(Streamlet<Trampoline> trs) {
		List<Trampoline> trs_ = trs.toList();
		if (trs_.size() == 0)
			return okay;
		else if (trs_.size() == 1)
			return trs_.get(0);
		else if (trs_.size() == 2) {
			Trampoline tr0 = trs_.get(0);
			Trampoline tr1 = trs_.get(1);
			return rt -> {
				rt.pushRem(tr1);
				return tr0;
			};
		} else {
			Trampoline trh = trs_.get(0);
			List<Trampoline> trt = Util.reverse(Util.right(trs_, 1));
			return rt -> {
				for (Trampoline tr_ : trt)
					rt.pushRem(tr_);
				return trh;
			};
		}
	}

	private Trampoline or(Streamlet<Trampoline> trs) {
		List<Trampoline> trs_ = trs.toList();
		if (trs_.size() == 0)
			return fail;
		else if (trs_.size() == 1)
			return trs_.get(0);
		else if (trs_.size() == 2) {
			Trampoline tr0 = trs_.get(0);
			Trampoline tr1 = trs_.get(1);
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
		} else {
			Trampoline trh = trs_.get(0);
			List<Trampoline> trt = Util.reverse(Util.right(trs_, 1));
			return rt -> {
				IList<Trampoline> rems0 = rt.rems;
				int pit = rt.journal.getPointInTime();
				for (Trampoline tr_ : trt)
					rt.pushAlt(rt_ -> {
						rt_.journal.undoBinds(pit);
						rt_.rems = rems0;
						return tr_;
					});
				return trh;
			};
		}
	}

	private int complexity(Node node) {
		node = node.finalNode();
		Tree tree = Tree.decompose(node);
		if (tree != null)
			return 1 + Math.max(complexity(tree.getLeft()), complexity(tree.getRight()));
		else
			return node instanceof Atom && SewingGeneralizerImpl.isVariable(((Atom) node).name) ? 0 : 1;
	}

	private List<Node> breakdown(Operator operator, Node node) {
		List<Node> list = new ArrayList<>();
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

	private boolean isTrace(Prototype prototype) {
		boolean isTrace;
		if (Suite.isProverTrace) {
			Node head = prototype.head;
			String name = head instanceof Atom ? ((Atom) head).name : null;

			isTrace = name != null //
					&& !name.startsWith("member") //
					&& !name.startsWith("rbt-");
		} else
			isTrace = false;
		return isTrace;
	}

}
