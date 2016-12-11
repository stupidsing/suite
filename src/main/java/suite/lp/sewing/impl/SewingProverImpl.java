package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.Suite;
import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.immutable.IList;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Trail;
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
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Mutable;
import suite.node.util.SuiteException;
import suite.node.util.TreeRewriter;
import suite.node.util.TreeUtil;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
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

	private enum TraceLevel {
		NONE, STACK, TRACE,
	}

	private SystemPredicates systemPredicates;
	private ListMultimap<Prototype, Rule> rules = new ListMultimap<>();
	private Map<Prototype, Mutable<Trampoline>> trampolinesByPrototype = new HashMap<>();

	private Env emptyEnvironment = new Env(new Reference[0]);

	private Trampoline okay = rt -> {
		throw new RuntimeException("Impossibly okay");
	};
	private Trampoline fail = rt -> {
		throw new RuntimeException("Impossibly fail");
	};

	private SewingBinder passThru = new SewingBinder() {
		public BiPredicate<BindEnv, Node> compileBind(Node node) {
			return (be, n) -> Binder.bind(node, n, be.getTrail());
		}

		public Fun<Env, Node> compile(Node node) {
			return env -> node.finalNode();
		}

		public Env env() {
			return emptyEnvironment;
		}
	};

	public interface Trampoline {
		public Trampoline prove(Runtime rt);
	}

	private class Debug {
		private String indent = "";
		private IList<Node> stack = IList.end();

		private Debug(String indent, IList<Node> stack) {
			this.indent = indent;
			this.stack = stack;
		}
	}

	private class Runtime implements BindEnv {
		private Env env = emptyEnvironment;
		private Node query;
		private IList<Trampoline> cutPoint;
		private IList<Trampoline> rems = IList.end(); // continuations
		private IList<Trampoline> alts = IList.end(); // alternatives
		private Trail trail = new Trail();
		private Prover prover;
		private Debug debug = new Debug("", IList.end());

		private Sink<Node> handler = node -> {
			throw new SuiteException(node, Read.from(debug.stack).map(Object::toString).collect(As.conc("\n")));
		};

		private Runtime(ProverConfig pc, Trampoline tr) {
			pushAlt(tr);
			prover = new Prover(pc, null, trail);
		}

		private void pushRem(Trampoline tr) {
			if (tr != okay)
				rems = IList.cons(tr, rems);
		}

		private void pushAlt(Trampoline tr) {
			alts = IList.cons(tr, alts);
		}

		public Env getEnv() {
			return env;
		}

		public Trail getTrail() {
			return trail;
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
			throw new RuntimeException("Must not contain wild rules");
	}

	public Predicate<ProverConfig> compile(Node node) {
		Trampoline tr = cutBegin(compile0(passThru, node));

		return pc -> {
			Mutable<Boolean> result = Mutable.of(false);

			trampoline(new Runtime(pc, rt -> {
				rt.pushRem(rt_ -> {
					result.update(true);
					return fail;
				});
				return tr;
			}));

			return result.get();
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

		rt.trail.unwindAll();
	}

	private void compileAll() {
		for (Pair<Prototype, List<Rule>> entry : rules.listEntries()) {
			Prototype prototype = entry.t0;
			TraceLevel traceLevel = traceLevel(prototype);

			List<Rule> rules = new ArrayList<>(entry.t1);
			Trampoline tr0 = compileRules(prototype, rules, traceLevel);
			Trampoline tr;

			// second-level indexing optimization
			if (6 <= rules.size()) {
				Map<Prototype, List<Rule>> rulesByProto1 = Read.from(rules) //
						.toListMap(rule -> Prototype.of(rule, 1));

				if (!rulesByProto1.containsKey(null)) {
					Map<Prototype, Trampoline> trByProto1 = Read.from2(rulesByProto1) //
							.mapValue(rules_ -> compileRules(prototype, rules_, traceLevel)) //
							.toMap();

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

			getTrampolineByPrototype(prototype).set(tr);
		}
	}

	private Trampoline compileRules(Prototype prototype, List<Rule> rules, TraceLevel traceLevel) {
		boolean hasCut = Read.from(rules) //
				.map(rule -> new TreeRewriter().contains(ProverConstant.cut, rule.tail)) //
				.fold(false, (b0, b1) -> b0 || b1);

		Streamlet<Trampoline> trs = Read.from(rules).map(rule -> {
			Generalizer generalizer = new Generalizer();
			Node head = generalizer.generalize(rule.head);
			Node tail = generalizer.generalize(rule.tail);
			return compileRule(head, tail);
		});

		Trampoline tr0 = or(trs);
		Trampoline tr1 = hasCut ? cutBegin(tr0) : tr0;
		Trampoline tr2 = saveEnv(tr1);
		return log(tr2, traceLevel);
	}

	private Trampoline compileRule(Node head, Node tail) {
		SewingBinder sb = new SewingBinderImpl();
		BiPredicate<BindEnv, Node> p = sb.compileBind(head);
		Trampoline tr1 = compile0(sb, tail);
		return newEnv(sb, rt -> p.test(rt, rt.query) ? tr1 : fail);
	}

	private Trampoline compile0(SewingBinder sb, Node node) {
		Trampoline tr = null;
		List<Node> list;
		Tree tree;
		Node m[];

		if (1 < (list = breakdown(TermOp.AND___, node)).size())
			tr = and(Read.from(list).map(n -> compile0(sb, n)));
		else if (1 < (list = breakdown(TermOp.OR____, node)).size())
			tr = or(Read.from(list).map(n -> compile0(sb, n)));
		else if ((m = Suite.matcher(".0 = .1").apply(node)) != null) {
			boolean b = complexity(m[0]) <= complexity(m[1]);
			Node n0 = b ? m[0] : m[1];
			Node n1 = b ? m[1] : m[0];
			BiPredicate<BindEnv, Node> p = sb.compileBind(n1);
			Fun<Env, Node> f = sb.compile(n0);
			tr = rt -> p.test(rt, f.apply(rt.env)) ? okay : fail;
		} else if ((m = Suite.matcher("builtin:.0:.1 .2").apply(node)) != null) {
			String className = ((Atom) m[0]).name;
			String fieldName = ((Atom) m[1]).name;
			BuiltinPredicate predicate = Rethrow.ex(() -> {
				Class<?> clazz = Class.forName(className);
				return (BuiltinPredicate) clazz.getField(fieldName).get(clazz.newInstance());
			});
			tr = callPredicate(sb, predicate, m[2]);
		} else if ((m = Suite.matcher("find.all .0 .1 .2").apply(node)) != null) {
			Fun<Env, Node> f = sb.compile(m[0]);
			Trampoline tr1 = compile0(sb, m[1]);
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[2]);
			List<Node> vs = new ArrayList<>();
			return rt -> {
				Sink<Runtime> restore = save(rt);
				rt.pushRem(rt_ -> {
					vs.add(new Cloner().clone(f.apply(rt_.env)));
					return fail;
				});
				rt.pushAlt(rt_ -> {
					restore.sink(rt_);
					return p.test(rt, Tree.of(TermOp.AND___, vs)) ? okay : fail;
				});
				return tr1;
			};
		} else if ((m = Suite.matcher("if .0 .1 .2").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			Trampoline tr1 = compile0(sb, m[1]);
			Trampoline tr2 = compile0(sb, m[2]);
			tr = if_(tr0, tr1, tr2);
		} else if ((m = Suite.matcher("let .0 .1").apply(node)) != null) {
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[0]);
			Evaluate eval = new SewingExpressionImpl(sb).compile(m[1]);
			tr = rt -> p.test(rt, Int.of(eval.evaluate(rt.env))) ? okay : fail;
		} else if ((m = Suite.matcher("list.fold .0/.1/.2 .3").apply(node)) != null) {
			Fun<Env, Node> list0_ = sb.compile(m[0]);
			Fun<Env, Node> value0_ = sb.compile(m[1]);
			BiPredicate<BindEnv, Node> valuex_ = sb.compileBind(m[2]);
			Fun<Env, Node> ht_ = sb.compile(m[3]);
			return rt -> {
				Node ht[] = Suite.matcher(".0 .1").apply(ht_.apply(rt.env));
				Trampoline tr1 = saveEnv(compileRule(ht[0], ht[1]));
				Mutable<Node> current = Mutable.of(value0_.apply(rt.env));
				rt.pushRem(rt_ -> valuex_.test(rt_, current.get()) ? okay : fail);
				for (Node elem : Tree.iter(list0_.apply(rt.env))) {
					Reference result = new Reference();
					rt.pushRem(rt_ -> {
						current.update(result.finalNode());
						return okay;
					});
					rt.pushRem(rt_ -> {
						rt_.query = Tree.of(TermOp.ITEM__, Tree.of(TermOp.ITEM__, elem, current.get()), result);
						return tr1;
					});
				}
				return okay;
			};
		} else if ((m = Suite.matcher("list.fold.clone .0/.1/.2 .3/.4/.5 .6").apply(node)) != null) {
			Fun<Env, Node> list0_ = sb.compile(m[0]);
			Fun<Env, Node> value0_ = sb.compile(m[1]);
			BiPredicate<BindEnv, Node> valuex_ = sb.compileBind(m[2]);
			BiPredicate<BindEnv, Node> elem_ = sb.compileBind(m[3]);
			BiPredicate<BindEnv, Node> v0_ = sb.compileBind(m[4]);
			Fun<Env, Node> vx_ = sb.compile(m[5]);
			Trampoline tr1 = compile0(sb, m[6]);
			return rt -> {
				Mutable<Node> current = Mutable.of(value0_.apply(rt.env));
				Env env0 = rt.env;
				rt.pushRem(rt_ -> {
					rt_.env = env0;
					return valuex_.test(rt_, current.get()) ? okay : fail;
				});
				for (Node elem : Tree.iter(list0_.apply(rt.env))) {
					rt.pushRem(rt_ -> {
						current.update(vx_.apply(rt_.env));
						return okay;
					});
					rt.pushRem(rt_ -> {
						rt_.env = env0.clone();
						return elem_.test(rt_, elem) && v0_.test(rt_, current.get()) ? tr1 : fail;
					});
				}
				return okay;
			};
		} else if ((m = Suite.matcher("list.query .0 .1").apply(node)) != null) {
			Fun<Env, Node> l_ = sb.compile(m[0]);
			Fun<Env, Node> ht_ = sb.compile(m[1]);
			return rt -> {
				Node ht[] = Suite.matcher(".0 .1").apply(ht_.apply(rt.env));
				Trampoline tr1 = saveEnv(compileRule(ht[0], ht[1]));
				for (Node n : Tree.iter(l_.apply(rt.env)))
					rt.pushRem(rt_ -> {
						rt_.query = n;
						return tr1;
					});
				return okay;
			};
		} else if ((m = Suite.matcher("list.query.clone .0 .1 .2").apply(node)) != null) {
			Fun<Env, Node> f = sb.compile(m[0]);
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[1]);
			Trampoline tr1 = compile0(sb, m[2]);
			return rt -> {
				Env env0 = rt.env;
				rt.pushRem(rt_ -> {
					rt_.env = env0;
					return okay;
				});
				for (Node n : Tree.iter(f.apply(rt.env)))
					rt.pushRem(rt_ -> {
						rt_.env = env0.clone();
						return p.test(rt_, n) ? tr1 : fail;
					});
				return okay;
			};
		} else if ((m = Suite.matcher("member .0 .1").apply(node)) != null && TreeUtil.isList(m[0], TermOp.AND___)) {
			List<BiPredicate<BindEnv, Node>> elems_ = Read.from(Tree.iter(m[0])).map(sb::compileBind).toList();
			Fun<Env, Node> f = sb.compile(m[1]);
			return rt -> {
				Iterator<BiPredicate<BindEnv, Node>> iter = elems_.iterator();
				Trampoline alt[] = new Trampoline[1];
				Sink<Runtime> restore = save(rt);
				return alt[0] = rt_ -> {
					while (iter.hasNext()) {
						restore.sink(rt);
						if (iter.next().test(rt_, f.apply(rt.env))) {
							rt_.pushAlt(alt[0]);
							return okay;
						}
					}
					return fail;
				};
			};
		} else if ((m = Suite.matcher("not .0").apply(node)) != null)
			tr = if_(compile0(sb, m[0]), fail, okay);
		else if ((m = Suite.matcher("once .0").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			tr = rt -> {
				IList<Trampoline> alts0 = rt.alts;
				rt.pushRem(rt_ -> {
					rt_.alts = alts0;
					return okay;
				});
				return tr0;
			};
		} else if ((m = Suite.matcher("suspend .0 .1 .2").apply(node)) != null) {
			Fun<Env, Node> f0 = sb.compile(m[0]);
			Fun<Env, Node> f1 = sb.compile(m[1]);
			Trampoline tr0 = compile0(sb, m[2]);

			tr = rt -> {
				List<Node> results = new ArrayList<>();
				Env env = rt.env;

				Trampoline tr_ = and(Read.from(tr0, rt_ -> {
					results.add(f1.apply(env));
					return fail;
				}));

				Node n0 = f0.apply(rt.env);

				Suspend suspend = new Suspend(() -> {
					Runtime rt_ = new Runtime(rt.prover.config(), tr_);
					rt_.env = env;
					trampoline(rt_);
					return Read.from(results).uniqueResult();
				});

				if (n0 instanceof Reference) {
					rt.trail.addBind((Reference) n0, suspend);
					return okay;
				} else
					return fail;
			};
		} else if ((m = Suite.matcher("throw .0").apply(node)) != null) {
			Fun<Env, Node> f = sb.compile(m[0]);
			tr = rt -> {
				rt.handler.sink(new Cloner().clone(f.apply(rt.env)));
				return okay;
			};
		} else if ((m = Suite.matcher("try .0 .1 .2").apply(node)) != null) {
			Trampoline tr0 = compile0(sb, m[0]);
			BiPredicate<BindEnv, Node> p = sb.compileBind(m[1]);
			Trampoline catch0 = compile0(sb, m[2]);
			tr = rt -> {
				BindEnv be = rt;
				Sink<Runtime> restore = save(rt);
				IList<Trampoline> alts0 = rt.alts;
				Sink<Node> handler0 = rt.handler;
				rt.handler = node_ -> {
					restore.sink(rt);
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
		} else if ((m = TreeUtil.tuple(node)) != null && m[0] instanceof Atom)
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
			return rt -> compile0(passThru, f.apply(rt.env));
		} else if (node instanceof Data<?>) {
			Object data = ((Data<?>) node).data;
			if (data instanceof Source<?>)
				tr = rt -> ((Source<?>) data).source() != Boolean.TRUE ? okay : fail;
		}

		if (tr == null) {
			Prototype prototype = Prototype.of(node);
			if (rules.containsKey(prototype)) {
				Fun<Env, Node> f = sb.compile(node);
				Mutable<Trampoline> mtr = getTrampolineByPrototype(prototype);
				tr = rt -> {
					rt.query = f.apply(rt.env);
					// logUtil.info(Formatter.dump(rt.query));
					return mtr.get()::prove;
				};
			}
		}

		if (tr != null)
			return tr;
		else
			throw new RuntimeException("Cannot understand " + node);
	}

	private Trampoline if_(Trampoline tr0, Trampoline tr1, Trampoline tr2) {
		return rt -> {
			Sink<Runtime> restore = save(rt);
			IList<Trampoline> alts0 = rt.alts;
			rt.pushRem(rt_ -> {
				rt_.alts = alts0;
				return tr1;
			});
			rt.pushAlt(rt_ -> {
				restore.sink(rt_);
				return tr2;
			});
			return tr0;
		};
	}

	private Trampoline callSystemPredicate(SewingBinder sb, String name, Node pass) {
		BuiltinPredicate predicate = systemPredicates.get(name);
		return predicate != null ? callPredicate(sb, predicate, pass) : null;
	}

	private Trampoline callPredicate(SewingBinder sb, BuiltinPredicate predicate, Node pass) {
		Fun<Env, Node> f = sb.compile(pass);
		return rt -> predicate.prove(rt.prover, f.apply(rt.env)) ? okay : fail;
	}

	private Trampoline saveEnv(Trampoline tr) {
		return rt -> {
			Env env0 = rt.env;
			rt.pushRem(rt_ -> {
				rt_.env = env0;
				return okay;
			});
			return tr;
		};
	}

	private Trampoline newEnv(SewingBinder sb, Trampoline tr) {
		return rt -> {
			rt.env = sb.env();
			return tr;
		};
	}

	private Trampoline cutBegin(Trampoline tr) {
		return rt -> {
			IList<Trampoline> cutPoint0 = rt.cutPoint;
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
				Debug debug0 = rt.debug;
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
				String m = Formatter.dump(rt.query);
				String indent = rt.debug.indent;

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
				Sink<Runtime> restore = save(rt);
				rt.pushAlt(rt_ -> {
					restore.sink(rt_);
					return tr1;
				});
				return tr0;
			};
		} else {
			Trampoline trh = trs_.get(0);
			List<Trampoline> trt = Util.reverse(Util.right(trs_, 1));
			return rt -> {
				Sink<Runtime> restore = save(rt);
				for (Trampoline tr_ : trt)
					rt.pushAlt(rt_ -> {
						restore.sink(rt_);
						return tr_;
					});
				return trh;
			};
		}
	}

	private Sink<Runtime> save(Runtime rt) {
		Env env0 = rt.env;
		Node query0 = rt.query;
		IList<Trampoline> cutPoint0 = rt.cutPoint;
		IList<Trampoline> rems0 = rt.rems;
		int pit0 = rt.trail.getPointInTime();
		Sink<Node> handler0 = rt.handler;
		return rt_ -> {
			rt_.env = env0;
			rt_.query = query0;
			rt_.cutPoint = cutPoint0;
			rt_.rems = rems0;
			rt_.trail.unwind(pit0);
			rt_.handler = handler0;
		};
	}

	private int complexity(Node node) {
		Tree tree = Tree.decompose(node);
		if (tree != null)
			return 1 + Math.max(complexity(tree.getLeft()), complexity(tree.getRight()));
		else
			return node instanceof Atom && SewingGeneralizerImpl.isVariable(((Atom) node).name) ? 0 : 1;
	}

	private List<Node> breakdown(Operator operator, Node node) {
		List<Node> list = new ArrayList<>();
		breakdown0(operator, node, list);
		return list;
	}

	private void breakdown0(Operator operator, Node node, List<Node> list) {
		Tree tree;
		while ((tree = Tree.decompose(node, operator)) != null) {
			breakdown0(operator, tree.getLeft(), list);
			node = tree.getRight();
		}
		list.add(node);
	}

	private Mutable<Trampoline> getTrampolineByPrototype(Prototype prototype) {
		return trampolinesByPrototype.computeIfAbsent(prototype, k -> Mutable.nil());
	}

	private TraceLevel traceLevel(Prototype prototype) {
		TraceLevel traceLevel;
		if (Suite.isProverTrace) {
			Node head = prototype.head;
			String name = head instanceof Atom ? ((Atom) head).name : null;

			traceLevel = name != null //
					&& !name.startsWith("member") //
					&& !name.startsWith("rbt-") ? TraceLevel.TRACE : TraceLevel.NONE;
		} else
			traceLevel = TraceLevel.NONE;
		return traceLevel;
	}

}
