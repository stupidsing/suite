package suite.lp.sewing.impl;

import static suite.util.Friends.fail;
import static suite.util.Friends.max;
import static suite.util.Friends.rethrow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.immutable.IList;
import suite.lp.Configuration.ProverCfg;
import suite.lp.compile.impl.CompileExpressionImpl;
import suite.lp.doer.Binder;
import suite.lp.doer.BinderFactory;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.Cloner;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.doer.ProverConstant;
import suite.lp.doer.ProverFactory;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.lp.predicate.SystemPredicates;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.util.Rewrite;
import suite.node.util.SuiteException;
import suite.node.util.TreeUtil;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.List_;
import suite.util.String_;

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
public class SewingProverImpl implements ProverFactory {

	private static Rewrite rw = new Rewrite();

	private enum TraceLevel {
		NONE, STACK, TRACE,
	}

	private SystemPredicates systemPredicates;
	private Map<Prototype, Boolean> isHasCutByPrototype;
	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Mutable<Cps>> cpsByPrototype = new HashMap<>();
	private Map<Prototype, Mutable<Trampoline>> trampolineByPrototype = new HashMap<>();

	private Env emptyEnvironment = Env.empty(0);

	private Trampoline okay = rt -> fail("impossibly okay");
	private Trampoline fail = rt -> fail("impossibly fail");

	private BinderFactory passThru = new BinderFactory() {
		public VariableMapper<Reference> mapper() {
			return new VariableMapper<>();
		}

		public Bind_ binder(Node node) {
			return (be, n) -> Binder.bind(node, n, be.trail);
		}

		public Clone_ cloner(Node node) {
			return env -> node.finalNode();
		}
	};

	public interface Cps {
		public Cps cont(Runtime rt);
	}

	public interface Trampoline {
		public Trampoline prove(Runtime rt);
	}

	private interface Restore {
		public void restore(Runtime rt);
	}

	private class Debug {
		private String indent = "";
		private IList<Node> stack = IList.end();

		private Debug(String indent, IList<Node> stack) {
			this.indent = indent;
			this.stack = stack;
		}
	}

	private class Runtime extends BindEnv {
		private Cps cps;
		private Node query;
		private IList<Trampoline> cutPoint;
		private IList<Trampoline> rems = IList.end(); // continuations
		private IList<Trampoline> alts = IList.end(); // alternatives
		private Prover prover;
		private Debug debug = new Debug("", IList.end());

		private void trampoline() {
			while (!alts.isEmpty()) {
				rems = IList.cons(alts.head, IList.end());
				alts = alts.tail;

				Trampoline rem;
				while ((rem = rems.head) != fail) {
					rems = rems.tail;
					pushRem(rem.prove(this));
				}
			}

			trail.unwindAll();
		}

		private Sink<Node> handler = node -> {
			throw new SuiteException(node, debug.stack.streamlet().map(Object::toString).collect(As.conc("\n")));
		};

		private Runtime(Runtime rt, Trampoline tr) {
			this(rt.env, rt.prover.config(), tr);
		}

		private Runtime(ProverCfg pc, Trampoline tr) {
			this(emptyEnvironment, pc, tr);
		}

		private Runtime(Env env, ProverCfg pc, Trampoline tr) {
			super(env);
			pushAlt(tr);
			prover = new Prover(pc, null, trail);
		}

		private void cont(Cps cps) {
			while (cps != null)
				cps = cps.cont(this);
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
		this(Prototype.multimap(rs));
	}

	public SewingProverImpl(ListMultimap<Prototype, Rule> rules) {
		this.rules = rules;
		systemPredicates = new SystemPredicates(null);

		if (!rules.containsKey(null))
			compileAll();
		else
			fail("must not contain wild rules");
	}

	public Prove_ prover(Node node) {
		var tr = cutBegin(compileTr(passThru, node));

		return pc -> {
			var result = Mutable.of(false);

			new Runtime(pc, rt -> {
				rt.pushRem(rt_ -> {
					result.update(true);
					return fail;
				});
				return tr;
			}).trampoline();

			return result.value();
		};
	}

	private void compileAll() {
		isHasCutByPrototype = rules.listEntries().mapValue(this::isHasCut).toMap();

		for (var e : rules.listEntries()) {
			var prototype = e.t0;
			var rules = new ArrayList<>(e.t1);
			var traceLevel = traceLevel(prototype);

			// second-level indexing optimization
			Map<Prototype, List<Rule>> rulesByProto1;

			if (6 <= rules.size()) {
				var rulesByProto_ = Read.from(rules).toListMap(rule -> Prototype.of(rule, 1));
				rulesByProto1 = !rulesByProto_.containsKey(null) ? rulesByProto_ : null;
			} else
				rulesByProto1 = null;

			if (isHasCutByPrototype.get(prototype)) {
				var tr0 = compileTrRules(prototype, rules, traceLevel);
				Trampoline tr;

				if (rulesByProto1 != null) {
					var trByProto1 = Read //
							.from2(rulesByProto1) //
							.mapValue(rules_ -> compileTrRules(prototype, rules_, traceLevel)) //
							.toMap();

					tr = rt -> {
						var proto = Prototype.of(rt.query, 1);
						if (proto != null) {
							var tr_ = trByProto1.get(proto);
							return tr_ != null ? tr_ : fail;
						} else
							return tr0;
					};
				} else
					tr = tr0;

				getTrampolineByPrototype(prototype).set(tr);
			} else {
				var cps0 = compileCpsRules(prototype, rules, traceLevel);
				Cps cps;

				if (rulesByProto1 != null) {
					var cpsByProto1 = Read //
							.from2(rulesByProto1) //
							.mapValue(rules_ -> compileCpsRules(prototype, rules_, traceLevel)) //
							.toMap();

					cps = rt -> {
						var proto = Prototype.of(rt.query, 1);
						return proto != null ? cpsByProto1.get(proto) : cps0;
					};
				} else
					cps = cps0;

				getCpsByPrototype(prototype).set(cps);
			}
		}
	}

	private boolean isHasCut(List<Rule> rules) {
		return Boolean.TRUE || Read //
				.from(rules) //
				.map(rule -> rw.contains(ProverConstant.cut, rule.tail)) //
				.isAny(b -> b);
	}

	private Cps compileCpsRules(Prototype prototype, List<Rule> rules, TraceLevel traceLevel) {
		var cpss = Read.from(rules).map(rule -> {
			var generalizer = new Generalizer();
			var head = generalizer.generalize(rule.head);
			var tail = generalizer.generalize(rule.tail);
			return compileCpsRule(head, tail);
		});

		var cps0 = orCps(cpss);
		return saveEnvCps(cps0);
	}

	private Cps compileCpsRule(Node head, Node tail) {
		var bf = new SewingBinderImpl();
		var p = bf.binder(head);
		var cps = compileCps(bf, tail, rt -> rt.cps);
		return newEnvCps(bf, rt -> p.test(rt, rt.query) ? cps : null);
	}

	private Cps compileCps(BinderFactory bf, Node node, Cps cpsx) {
		Streamlet<Node> list;
		Tree tree;
		Node m[];
		Cps cps;

		if (1 < (list = TreeUtil.breakdown(TermOp.AND___, node)).size()) {
			cps = cpsx;
			for (var n : list.reverse())
				cps = compileCps(bf, n, cps);
		} else if (1 < (list = TreeUtil.breakdown(TermOp.OR____, node)).size())
			cps = orCps(list.map(n -> compileCps(bf, n, cpsx)));
		else if ((m = Suite.pattern(".0 = .1").match(node)) != null) {
			var b = complexity(m[0]) <= complexity(m[1]);
			var n0 = b ? m[0] : m[1];
			var n1 = b ? m[1] : m[0];
			var p = bf.binder(n1);
			var f = bf.cloner(n0);
			cps = rt -> p.test(rt, f.apply(rt.env)) ? cpsx : null;
		} else if ((m = Suite.pattern(".0 .1").match(node)) != null && m[0] instanceof Atom)
			cps = compileCpsCallPredicate(bf, Atom.name(m[0]), m[1], node, cpsx);
		else if (node instanceof Atom) {
			var name = Atom.name(node);
			if (String_.equals(name, ""))
				cps = cpsx;
			else if (String_.equals(name, "fail"))
				cps = rt -> null;
			else
				cps = compileCpsCallPredicate(bf, name, Atom.NIL, node, cpsx);
		} else if (node instanceof Reference) {
			var f = bf.cloner(node);
			cps = rt -> compileCps(passThru, f.apply(rt.env), cpsx);
		} else if ((tree = Tree.decompose(node)) != null)
			cps = compileCpsCallPredicate(bf, tree.getOperator().name_(), node, node, cpsx);
		else if (node instanceof Tuple)
			cps = compileCpsCallPredicate(bf, node, cpsx);
		else
			cps = fail("cannot understand " + node);

		return cps;
	}

	private Cps orCps(Streamlet<Cps> cpss) {
		var cpsList = cpss.toList();
		var cpsArray = List_.left(cpsList, -1).toArray(new Cps[0]);
		var cps_ = List_.last(cpsList);
		return rt -> {
			var restore = save(rt);
			for (var cps1 : cpsArray) {
				rt.cont(cps1);
				restore.restore(rt);
			}
			return cps_;
		};
	}

	private Cps compileCpsCallPredicate(BinderFactory bf, String name, Node pass, Node node, Cps cpsx) {
		var predicate = systemPredicates.get(name);
		return predicate != null ? compileCpsCallPredicate(bf, predicate, pass, cpsx) : compileCpsCallPredicate(bf, node, cpsx);
	}

	private Cps compileCpsCallPredicate(BinderFactory bf, BuiltinPredicate predicate, Node pass, Cps cpsx) {
		var f = bf.cloner(pass);
		return rt -> predicate.prove(rt.prover, f.apply(rt.env)) ? cpsx : null;
	}

	private Cps compileCpsCallPredicate(BinderFactory bf, Node node, Cps cpsx) {
		var prototype = Prototype.of(node);
		if (rules.containsKey(prototype)) {
			var f = bf.cloner(node);
			Cps cps;
			if (isHasCutByPrototype.get(prototype)) {
				var mtr = getTrampolineByPrototype(prototype);
				Trampoline rem = rt -> {
					rt.cont(cpsx);
					return fail;
				};
				cps = rt -> {
					var rems = rt.rems;
					rt.rems = IList.cons(fail, IList.end());
					new Runtime(rt, rt_ -> {
						rt_.query = f.apply(rt.env);
						rt_.rems = rems;
						rt_.pushRem(rem);
						return mtr.value();
					}).trampoline();
					return null;
				};
			} else {
				var mcps = getCpsByPrototype(prototype);
				cps = rt -> {
					var cps0 = rt.cps;
					rt.cps = rt_ -> {
						rt.cps = cps0;
						return cpsx;
					};
					rt.query = f.apply(rt.env);
					return mcps.value();
				};
			}
			return cps;
		} else
			return fail("cannot find predicate " + prototype);
	}

	private Cps saveEnvCps(Cps cps) {
		return rt -> {
			var cps0 = rt.cps;
			var env0 = rt.env;
			rt.cps = rt_ -> {
				rt.env = env0;
				return cps0;
			};
			return cps;
		};
	}

	private Cps newEnvCps(BinderFactory bf, Cps cps) {
		var mapper = bf.mapper();
		return rt -> {
			rt.env = mapper.env();
			return cps;
		};
	}

	private Trampoline compileTrRules(Prototype prototype, List<Rule> rules, TraceLevel traceLevel) {
		var trs = Read.from(rules).map(rule -> {
			var generalizer = new Generalizer();
			var head = generalizer.generalize(rule.head);
			var tail = generalizer.generalize(rule.tail);
			return compileTrRule(head, tail);
		});

		var tr0 = orTr(trs);
		var tr1 = cutBegin(tr0);
		var tr2 = saveEnvTr(tr1);
		return log(tr2, traceLevel);
	}

	private Trampoline compileTrRule(Node head, Node tail) {
		var bf = new SewingBinderImpl();
		var p = bf.binder(head);
		var tr1 = compileTr(bf, tail);
		return newEnvTr(bf, rt -> p.test(rt, rt.query) ? tr1 : fail);
	}

	private Trampoline compileTr(BinderFactory bf, Node node) {
		Streamlet<Node> list;
		Trampoline tr;
		Tree tree;
		Node[] m;

		if (1 < (list = TreeUtil.breakdown(TermOp.AND___, node)).size())
			tr = andTr(list.map(n -> compileTr(bf, n)));
		else if (1 < (list = TreeUtil.breakdown(TermOp.OR____, node)).size())
			tr = orTr(list.map(n -> compileTr(bf, n)));
		else if ((m = Suite.pattern(".0 = .1").match(node)) != null) {
			var b = complexity(m[0]) <= complexity(m[1]);
			var n0 = b ? m[0] : m[1];
			var n1 = b ? m[1] : m[0];
			var p = bf.binder(n1);
			var f = bf.cloner(n0);
			tr = rt -> p.test(rt, f.apply(rt.env)) ? okay : fail;
		} else if ((m = Suite.pattern("builtin:.0:.1 .2").match(node)) != null) {
			var className = Atom.name(m[0]);
			var fieldName = Atom.name(m[1]);
			BuiltinPredicate predicate = rethrow(() -> {
				var clazz = Class.forName(className);
				return (BuiltinPredicate) clazz.getField(fieldName).get(Object_.new_(clazz));
			});
			tr = compileTrCallPredicate(bf, predicate, m[2]);
		} else if ((m = Suite.pattern("find.all .0 .1 .2").match(node)) != null) {
			var f = bf.cloner(m[0]);
			var tr1 = compileTr(bf, m[1]);
			var p = bf.binder(m[2]);
			var vs = new ArrayList<Node>();
			tr = rt -> {
				var restore = save(rt);
				rt.pushRem(rt_ -> {
					vs.add(new Cloner().clone(f.apply(rt_.env)));
					return fail;
				});
				rt.pushAlt(rt_ -> {
					restore.restore(rt);
					return p.test(rt, TreeUtil.buildUp(TermOp.AND___, vs)) ? okay : fail;
				});
				return tr1;
			};
		} else if ((m = Suite.pattern("if .0 .1 .2").match(node)) != null) {
			var tr0 = compileTr(bf, m[0]);
			var tr1 = compileTr(bf, m[1]);
			var tr2 = compileTr(bf, m[2]);
			tr = if_(tr0, tr1, tr2);
		} else if ((m = Suite.pattern("let .0 .1").match(node)) != null) {
			var p = bf.binder(m[0]);
			var eval = new CompileExpressionImpl(bf).evaluator(m[1]);
			tr = rt -> p.test(rt, Int.of(eval.evaluate(rt.env))) ? okay : fail;
		} else if ((m = Suite.pattern("list.fold .0/.1/.2 .3").match(node)) != null) {
			var list0_ = bf.cloner(m[0]);
			var value0_ = bf.cloner(m[1]);
			var valuex_ = bf.binder(m[2]);
			var ht_ = bf.cloner(m[3]);
			tr = rt -> {
				var ht = Suite.pattern(".0 .1").match(ht_.apply(rt.env));
				var tr1 = saveEnvTr(compileTrRule(ht[0], ht[1]));
				var current = Mutable.of(value0_.apply(rt.env));
				rt.pushRem(rt_ -> valuex_.test(rt_, current.value()) ? okay : fail);
				for (var elem : Tree.iter(list0_.apply(rt.env))) {
					var result = new Reference();
					rt.pushRem(rt_ -> {
						current.update(result.finalNode());
						return okay;
					});
					rt.pushRem(rt_ -> {
						rt_.query = Tree.of(TermOp.ITEM__, Tree.of(TermOp.ITEM__, elem, current.value()), result);
						return tr1;
					});
				}
				return okay;
			};
		} else if ((m = Suite.pattern("list.query .0 .1").match(node)) != null) {
			var l_ = bf.cloner(m[0]);
			var ht_ = bf.cloner(m[1]);
			tr = rt -> {
				var ht = Suite.pattern(".0 .1").match(ht_.apply(rt.env));
				var tr1 = saveEnvTr(compileTrRule(ht[0], ht[1]));
				for (var n : Tree.iter(l_.apply(rt.env)))
					rt.pushRem(rt_ -> {
						rt_.query = n;
						return tr1;
					});
				return okay;
			};
		} else if ((m = Suite.pattern("member .0 .1").match(node)) != null && TreeUtil.isList(m[0], TermOp.AND___)) {
			var elems_ = Tree.iter(m[0]).map(bf::binder).toList();
			var f = bf.cloner(m[1]);
			tr = rt -> {
				var iter = elems_.iterator();
				var alt = new Trampoline[1];
				var restore = save(rt);
				return alt[0] = rt_ -> {
					while (iter.hasNext()) {
						restore.restore(rt);
						if (iter.next().test(rt_, f.apply(rt.env))) {
							rt_.pushAlt(alt[0]);
							return okay;
						}
					}
					return fail;
				};
			};
		} else if ((m = Suite.pattern("not .0").match(node)) != null)
			tr = if_(compileTr(bf, m[0]), fail, okay);
		else if ((m = Suite.pattern("once .0").match(node)) != null) {
			var tr0 = compileTr(bf, m[0]);
			tr = rt -> {
				var alts0 = rt.alts;
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return okay;
				});
				return tr0;
			};
		} else if ((m = Suite.pattern("suspend .0 .1 .2").match(node)) != null) {
			var f0 = bf.cloner(m[0]);
			var f1 = bf.cloner(m[1]);
			var tr0 = compileTr(bf, m[2]);

			tr = rt -> {
				var results = new ArrayList<Node>();
				var env = rt.env;

				var tr_ = andTr(Read.each(tr0, rt_ -> {
					results.add(f1.apply(env));
					return fail;
				}));

				var n0 = f0.apply(rt.env);

				var suspend = new Suspend(() -> {
					var rt_ = new Runtime(rt, tr_);
					rt_.trampoline();
					return Read.from(results).uniqueResult();
				});

				if (n0 instanceof Reference) {
					rt.trail.addBind((Reference) n0, suspend);
					return okay;
				} else
					return fail;
			};
		} else if ((m = Suite.pattern("throw .0").match(node)) != null) {
			var f = bf.cloner(m[0]);
			tr = rt -> {
				rt.handler.sink(new Cloner().clone(f.apply(rt.env)));
				return okay;
			};
		} else if ((m = Suite.pattern("try .0 .1 .2").match(node)) != null) {
			var tr0 = compileTr(bf, m[0]);
			var p = bf.binder(m[1]);
			var catch0 = compileTr(bf, m[2]);
			tr = rt -> {
				var be = rt;
				var restore = save(rt);
				var alts0 = rt.alts;
				var handler0 = rt.handler;
				rt.handler = node_ -> {
					restore.restore(rt);
					if (p.test(be, node_)) {
						rt.alts = alts0;
						rt.pushRem(catch0);
					} else
						handler0.sink(node_);
				};
				rt.pushRem(rt_ -> {
					rt_.handler = handler0;
					return okay;
				});
				return tr0;
			};
		} else if ((m = Suite.pattern(".0 .1").match(node)) != null && m[0] instanceof Atom)
			tr = compileTrCallPredicate(bf, Atom.name(m[0]), m[1], node);
		else if (node instanceof Atom) {
			var name = Atom.name(node);
			if (node == ProverConstant.cut)
				tr = cutEnd();
			else if (String_.equals(name, ""))
				tr = okay;
			else if (String_.equals(name, "fail"))
				tr = fail;
			else
				tr = compileTrCallPredicate(bf, name, Atom.NIL, node);
		} else if (node instanceof Data<?>) {
			var data = ((Data<?>) node).data;
			if (data instanceof Source<?>)
				tr = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
			else
				tr = fail("cannot understand " + node);
		} else if (node instanceof Reference) {
			var f = bf.cloner(node);
			tr = rt -> compileTr(passThru, f.apply(rt.env));
		} else if ((tree = Tree.decompose(node)) != null)
			tr = compileTrCallPredicate(bf, tree.getOperator().name_(), node, node);
		else if (node instanceof Tuple)
			tr = compileTrCallPredicate(bf, node);
		else
			tr = fail("cannot understand " + node);

		return tr;
	}

	private Trampoline andTr(Streamlet<Trampoline> trs) {
		var trs_ = trs.toList();
		if (trs_.size() == 0)
			return okay;
		else if (trs_.size() == 1)
			return trs_.get(0);
		else if (trs_.size() == 2) {
			var tr0 = trs_.get(0);
			var tr1 = trs_.get(1);
			return rt -> {
				rt.pushRem(tr1);
				return tr0;
			};
		} else {
			var trh = trs_.get(0);
			var trt = List_.reverse(List_.right(trs_, 1));
			return rt -> {
				for (var tr_ : trt)
					rt.pushRem(tr_);
				return trh;
			};
		}
	}

	private Trampoline orTr(Streamlet<Trampoline> trs) {
		var trs_ = trs.toList();
		if (trs_.size() == 0)
			return fail;
		else if (trs_.size() == 1)
			return trs_.get(0);
		else if (trs_.size() == 2) {
			var tr0 = trs_.get(0);
			var tr1 = trs_.get(1);
			return rt -> {
				var restore = save(rt);
				rt.pushAlt(rt_ -> {
					restore.restore(rt);
					return tr1;
				});
				return tr0;
			};
		} else {
			var trh = trs_.get(0);
			var trt = List_.reverse(List_.right(trs_, 1));
			return rt -> {
				var restore = save(rt);
				for (var tr_ : trt)
					rt.pushAlt(rt_ -> {
						restore.restore(rt);
						return tr_;
					});
				return trh;
			};
		}
	}

	private Trampoline if_(Trampoline tr0, Trampoline tr1, Trampoline tr2) {
		return rt -> {
			var restore = save(rt);
			var alts0 = rt.alts;
			rt.pushRem(rt_ -> {
				rt.alts = alts0;
				return tr1;
			});
			rt.pushAlt(rt_ -> {
				restore.restore(rt);
				return tr2;
			});
			return tr0;
		};
	}

	private Trampoline compileTrCallPredicate(BinderFactory bf, String name, Node pass, Node node) {
		var predicate = systemPredicates.get(name);
		return predicate != null ? compileTrCallPredicate(bf, predicate, pass) : compileTrCallPredicate(bf, node);
	}

	private Trampoline compileTrCallPredicate(BinderFactory bf, BuiltinPredicate predicate, Node pass) {
		var f = bf.cloner(pass);
		return rt -> predicate.prove(rt.prover, f.apply(rt.env)) ? okay : fail;
	}

	private Trampoline compileTrCallPredicate(BinderFactory bf, Node node) {
		var prototype = Prototype.of(node);
		if (rules.containsKey(prototype)) {
			var f = bf.cloner(node);
			Trampoline tr;
			if (isHasCutByPrototype.get(prototype)) {
				var mtr = getTrampolineByPrototype(prototype);
				tr = rt -> {
					rt.query = f.apply(rt.env);
					return mtr.value()::prove;
				};
			} else {
				var mcps = getCpsByPrototype(prototype);

				Cps cpsx = rt -> {
					var rems = rt.rems;
					rt.rems = IList.cons(fail, IList.end());
					new Runtime(rt, rt_ -> {
						rt_.rems = rems;
						return okay;
					}).trampoline();
					return null;
				};

				tr = rt -> {
					var cps0 = rt.cps;
					rt.cps = rt_ -> {
						rt.cps = cps0;
						return cpsx;
					};
					rt.query = f.apply(rt.env);
					rt.cont(mcps.value());
					return fail;
				};
			}
			return tr;
		} else
			return fail("cannot find predicate " + prototype);
	}

	private Trampoline saveEnvTr(Trampoline tr) {
		return rt -> {
			var env0 = rt.env;
			rt.pushRem(rt_ -> {
				rt_.env = env0;
				return okay;
			});
			return tr;
		};
	}

	private Trampoline newEnvTr(BinderFactory bf, Trampoline tr) {
		var mapper = bf.mapper();
		return rt -> {
			rt.env = mapper.env();
			return tr;
		};
	}

	private Trampoline cutBegin(Trampoline tr) {
		return rt -> {
			var cutPoint0 = rt.cutPoint;
			rt.pushRem(rt_ -> {
				rt_.cutPoint = cutPoint0;
				return okay;
			});
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

	private Trampoline log(Trampoline tr0, TraceLevel traceLevel) {
		Trampoline tr1, tr2;

		if (traceLevel == TraceLevel.STACK || traceLevel == TraceLevel.TRACE)
			tr1 = rt -> {
				var debug0 = rt.debug;
				rt.debug = new Debug(debug0.indent + "| ", IList.cons(rt.query, rt.debug.stack));
				rt.pushRem(rt2 -> {
					rt.debug = debug0;
					return okay;
				});
				rt.pushAlt(rt1 -> {
					rt.debug = debug0;
					return fail;
				});
				return tr0;
			};
		else
			tr1 = tr0;

		if (traceLevel == TraceLevel.TRACE)
			tr2 = rt -> {
				var m = Formatter.dump(rt.query);
				var indent = rt.debug.indent;

				LogUtil.info(indent + "QUERY " + m);
				rt.pushRem(rt_ -> {
					LogUtil.info(indent + "OK___ " + m);
					return okay;
				});
				rt.pushAlt(rt_ -> {
					LogUtil.info(indent + "FAIL_ " + m);
					return fail;
				});
				return tr1;
			};
		else
			tr2 = tr1;

		return tr2;
	}

	private Restore save(Runtime rt) {
		var cps0 = rt.cps;
		var env0 = rt.env;
		var query0 = rt.query;
		var cutPoint0 = rt.cutPoint;
		var rems0 = rt.rems;
		var pit0 = rt.trail.getPointInTime();
		var handler0 = rt.handler;
		return rt_ -> {
			rt_.cps = cps0;
			rt_.env = env0;
			rt_.query = query0;
			rt_.cutPoint = cutPoint0;
			rt_.rems = rems0;
			rt_.trail.unwind(pit0);
			rt_.handler = handler0;
		};
	}

	private int complexity(Node node) {
		var tree = Tree.decompose(node);
		if (tree != null)
			return 1 + max(complexity(tree.getLeft()), complexity(tree.getRight()));
		else
			return node instanceof Atom && ProverConstant.isVariable(Atom.name(node)) ? 0 : 1;
	}

	private Mutable<Cps> getCpsByPrototype(Prototype prototype) {
		return cpsByPrototype.computeIfAbsent(prototype, k -> Mutable.nil());
	}

	private Mutable<Trampoline> getTrampolineByPrototype(Prototype prototype) {
		return trampolineByPrototype.computeIfAbsent(prototype, k -> Mutable.nil());
	}

	private TraceLevel traceLevel(Prototype prototype) {
		TraceLevel traceLevel;
		if (Suite.isProverTrace) {
			var head = prototype.head;
			var name = head instanceof Atom ? Atom.name(head) : null;

			traceLevel = name != null //
					&& !name.startsWith("member") //
					&& !name.startsWith("rbt-") ? TraceLevel.TRACE : TraceLevel.NONE;
		} else
			traceLevel = TraceLevel.NONE;
		return traceLevel;
	}

}
