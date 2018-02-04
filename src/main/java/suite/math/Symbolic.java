package suite.math;

import java.util.ArrayList;
import java.util.List;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.Opt;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaInstance;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Symbolic {

	private FunFactory f = new FunFactory();
	private Int N0 = Int.of(0);
	private Int N1 = Int.of(1);

	public class PolynomializeException extends RuntimeException {
		private static final long serialVersionUID = 1l;
	}

	public Dbl_Dbl fun(Node fn, Node node0) {
		return LambdaInstance.of(Dbl_Dbl.class, x -> new Object() {
			public FunExpr fun(Node n) {
				return n == node0 ? x : m(n, this::fun);
			}
		}.fun(fn)).newFun();
	}

	public Obj_Dbl<float[]> fun(Node fn, Node[] vars) {
		@SuppressWarnings("unchecked")
		Obj_Dbl<float[]> fun = LambdaInstance.of(Obj_Dbl.class, x -> new Object() {
			public FunExpr fun(Node n) {
				for (int i = 0; i < vars.length; i++)
					if (n == vars[i])
						return x.index(f.int_(i));
				return m(n, this::fun);
			}
		}.fun(fn)).newFun();

		return fun;
	}

	private FunExpr m(Node n, Fun<Node, FunExpr> fun) {
		return new SwitchNode<FunExpr>(n //
		).match(patAdd, m -> {
			return f.bi("+", fun.apply(m[0]), fun.apply(m[1]));
		}).match(patNeg, m -> {
			return f.bi("-", f.double_(0d), fun.apply(m[0]));
		}).match(patMul, m -> {
			return f.bi("*", fun.apply(m[0]), fun.apply(m[1]));
		}).match(patInv, m -> {
			return f.bi("/", f.double_(1d), fun.apply(m[0]));
		}).match(patPow, m -> {
			return f.invokeStatic(Math.class, "pow", fun.apply(m[0]), fun.apply(m[1]));
		}).match(patExp, m -> {
			return f.invokeStatic(Math.class, "exp", fun.apply(m[0]));
		}).match(patLn, m -> {
			return f.invokeStatic(Math.class, "log", fun.apply(m[0]));
		}).match(patSin, m -> {
			return f.invokeStatic(Math.class, "sin", fun.apply(m[0]));
		}).match(patCos, m -> {
			return f.invokeStatic(Math.class, "cos", fun.apply(m[0]));
		}).applyIf(Int.class, i -> {
			return f.double_(i.number);
		}).nonNullResult();
	}

	public Node d(Node node0, Node x) {
		Rewrite rewrite = new Rewrite(x);
		Node node1 = rewrite.rewrite(node0);
		Node node2 = Boolean.TRUE ? rewrite.d(node1) : rewrite.i(node1).get();
		Node node3 = rewrite.simplify(node2);
		return node3;
	}

	public Node simplify(Node node, Node x) {
		return new Rewrite(x).simplify(node);
	}

	public Node simplify(Node node, Node... xs) {
		return simplify(node, xs, 0);
	}

	private Node simplify(Node node, Node[] xs, int i) {
		if (i < xs.length)
			return new Rewrite(xs[i]).polyize(node, coeff -> simplify(coeff, xs, i + 1)).or(() -> node);
		else
			return node;
	}

	private class Rewrite {
		private Node x;

		private Rewrite(Node x) {
			this.x = x;
		}

		// allowed:
		// id(PLUS__) := N0
		// id(MULT__) := N1
		// PLUS__ := Tree.of(PLUS__, a, b)
		// MULT__ := Tree.of(MULT__, a, b)
		// inverse of PLUS__ := neg a
		// inverse of MULT__ := inv a
		// exp a
		// ln a
		// sin a
		// i
		private Node rewrite(Node node) {
			return new SwitchNode<Node>(node //
			).match(".0 - .1", m -> {
				return m[0] != N0 ? add.apply(rewrite(m[0]), add.inverse(rewrite(m[1]))) : null;
			}).match(".0 / .1", m -> {
				return m[0] != N1 ? mul.apply(rewrite(m[0]), mul.inverse(rewrite(m[1]))) : null;
			}).match(patPow, m -> {
				return patExp.subst(patLn.subst(rewrite(m[0])), rewrite(m[1]));
			}).applyIf(Int.class, i -> {
				return intOf(i);
			}).match(".0 .1", m -> {
				return Suite.pattern(".0 .1").subst(m[0], rewrite(m[1]));
			}).applyTree((op, l, r) -> {
				return Tree.of(op, rewrite(l), rewrite(r));
			}).applyIf(Node.class, n -> {
				return n;
			}).nonNullResult();
		}

		private Node sumOfProducts(Node node) {
			class Recurse {
				private Streamlet<Node> pos(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match(patMul, m -> {
						return Streamlet.concat(pos(m[0]), pos(m[1]));
					}).match(patInv, m -> {
						return pos(m[0]).map(inv::apply);
					}).match(patPow, m -> {
						if (m[1] instanceof Int) {
							int power = ((Int) m[1]).number;
							int div2 = power / 2;
							int mod2 = power % 2;
							if (power < 0)
								return pos(mul.inverse(patPow.subst(m[0], Int.of(-power))));
							else if (power == 0) // TODO m[0] != 0
								return Read.empty();
							else {
								Streamlet<Node> n0 = pos(patPow.subst(m[0], Int.of(div2)));
								Streamlet<Node> n1 = Streamlet.concat(n0, n0);
								return mod2 != 0 ? Streamlet.concat(n1, pos(node_)) : n1;
							}
						} else
							return pos(m[0]).join2(sop(m[1])).map(patPow::subst);
					}).match(patExp, m -> {
						return sop(m[0]).map(patExp::subst);
					}).applyTree((op, l, r) -> {
						return Read.each(sumOfProducts(node_));
					}).applyIf(Node.class, n -> {
						return node_ == N1 ? Read.empty() : Read.each(node_);
					}).nonNullResult();
				}

				private Streamlet<Node> sop(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match(patAdd, m -> {
						return Streamlet.concat(sop(m[0]), sop(m[1]));
					}).match(patNeg, m -> {
						return sop(m[0]).map(neg::apply);
					}).match(patMul, m -> {
						return sop(m[0]).join2(sop(m[1])).map(mul::apply).map(this::productOfSums);
					}).match(patPow, m -> {
						return sop(productOfSums(node_));
					}).match(patLn, m -> {
						return pos(m[0]).map(patLn::subst);
					}).match("sin (.0 + .1)", m -> {
						return Read.each( //
								mul.recompose(x, Read.each(patSin.subst(m[0]), patCos.subst(m[1]))), //
								mul.recompose(x, Read.each(patCos.subst(m[0]), patSin.subst(m[1]))));
					}).match("cos (.0 + .1)", m -> {
						return Read.each( //
								mul.recompose(x, Read.each(patCos.subst(m[0]), patCos.subst(m[1]))), //
								mul.recompose(x, Read.each(neg.apply(patSin.subst(m[0])), patSin.subst(m[1]))));
					}).applyTree((op, l, r) -> {
						return Read.each(productOfSums(node_));
					}).applyIf(Node.class, n -> {
						return node_ == N0 ? Read.empty() : Read.each(node_);
					}).nonNullResult();
				}

				private Node productOfSums(Node node) {
					return mul.recompose(x, pos(node));
				}

				private Node sumOfProducts(Node node) {
					return add.recompose(x, sop(node));
				}

				private Iterate<Node> neg = add::inverse;
				private Iterate<Node> inv = mul::inverse;
			}

			return new Recurse().sumOfProducts(node);
		}

		private Node simplify(Node node) {
			return polyize(node, coeff -> coeff).or(() -> sumOfProducts(node));
		}

		private Opt<Node> polyize(Node node, Fun<Node, Node> coefficientFun) { // polynomialize
			class Map_ extends IntObjMap<Node> {
				Map_(IntObjStreamlet<Node> map) {
					map.sink(this::put);
				}

				Map_(int p, Node t) {
					put(p, t);
				}

				Map_() {
				}

				private void add(int power, Node term) {
					update(power, t -> add.apply(t != null ? t : N0, term));
				}
			}

			Opt<Map_> poly = new Object() {
				private Opt<Map_> poly(Node node) {
					return new SwitchNode<Opt<Map_>>(node //
					).match(patAdd, m -> {
						return poly(m[0]).join(poly(m[1]), (map0, map1) -> {
							Map_ map = new Map_();
							for (IntObjPair<Node> pair : IntObjStreamlet.concat(map0.streamlet(), map1.streamlet()))
								map.add(pair.t0, pair.t1);
							return map;
						});
					}).match(patNeg, m -> {
						return poly(m[0]).map(map -> {
							return new Map_(map.streamlet().mapIntObj((p, t) -> p, (p, t) -> add.inverse(t)));
						});
					}).match(patMul, m -> {
						return multiply(poly(m[0]), poly(m[1]));
					}).match(patInv, m -> {
						return poly(m[0]).concatMap(this::inv);
					}).match(patPow, m -> {
						return m[1] instanceof Int ? pow(m[0], ((Int) m[1]).number) : null;
					}).applyIf(Node.class, n -> {
						if (is_x(node)) {
							return Opt.of(new Map_(1, N1));
						} else if (node == N0)
							return Opt.of(new Map_());
						else if (!isContains_x(node)) {
							return Opt.of(new Map_(0, node));
						} else
							return Opt.none();
					}).nonNullResult();
				}

				private Opt<Map_> multiply(Opt<Map_> opt0, Opt<Map_> opt1) {
					return opt0.join(opt1, (map0, map1) -> {
						Map_ map = new Map_();
						for (IntObjPair<Node> pair0 : map0.streamlet())
							for (IntObjPair<Node> pair1 : map1.streamlet())
								map.add(pair0.t0 + pair1.t0, mul.apply(pair0.t1, pair1.t1));
						return map;
					});
				}

				private Opt<Map_> pow(Node m0, int power) {
					return polyize(m0, coeff -> coeff).concatMap(n -> {
						if (power < 0)
							return pow(n, -power).concatMap(this::inv);
						else if (power == 0) { // TODO assumed m[0] != 0
							return Opt.of(new Map_());
						} else {
							int div2 = power / 2;
							int mod2 = power % 2;
							Opt<Map_> opt0 = pow(m0, div2);
							Opt<Map_> opt1 = multiply(opt0, opt0);
							return mod2 != 0 ? multiply(opt1, poly(n)) : opt1;
						}
					});
				}

				private Opt<Map_> inv(Map_ map) {
					return map.size() == 1 //
							? Opt.of(new Map_(map.streamlet().mapIntObj((p, t) -> -p, (p, t) -> mul.inverse(t)))) //
							: Opt.none();
				}
			}.poly(node);

			Int_Obj<Node> powerFun = p -> {
				Node power = N1;
				for (int i = 0; i < p; i++)
					power = mul.apply(x, power);
				return power;
			};

			return poly.map(map -> {
				Node sum = N0;
				for (IntObjPair<Node> pair : map.streamlet().sortByKey(Integer::compare)) {
					int p = pair.t0;
					Node power = p < 0 ? mul.inverse(powerFun.apply(-p)) : powerFun.apply(p);
					sum = add.apply(mul.apply(coefficientFun.apply(pair.t1), power), sum);
				}
				return sum;
			});
		}

		private Node d(Node node) { // differentiation
			return new SwitchNode<Node>(node //
			).match(patAdd, m -> {
				return add.apply(d(m[0]), d(m[1]));
			}).match(patNeg, m -> {
				return add.inverse(d(m[0]));
			}).match(patMul, m -> {
				return add.apply(mul.apply(m[0], d(m[1])), mul.apply(m[1], d(m[0])));
			}).match(patInv, m -> {
				return mul.apply(mul.inverse(mul.apply(m[0], m[0])), add.inverse(d(m[0])));
			}).match(patExp, m -> {
				return mul.apply(patExp.subst(m[0]), d(m[0]));
			}).match(patLn, m -> {
				return mul.apply(mul.inverse(m[0]), d(m[0]));
			}).match(patSin, m -> {
				return mul.apply(patCos.subst(m[0]), d(m[0]));
			}).match(patCos, m -> {
				return mul.apply(add.inverse(patSin.subst(m[0])), d(m[0]));
			}).applyIf(Node.class, n -> {
				if (is_x(node))
					return N1;
				else if (node instanceof Int)
					return N0;
				else
					return null;
			}).nonNullResult();
		}

		private Opt<Node> i(Node node) { // integration
			return new SwitchNode<Opt<Node>>(node //
			).match(patAdd, m -> {
				Opt<Node> iudxs = i(m[0]);
				Opt<Node> ivdxs = i(m[1]);
				return iudxs.join(ivdxs, add::apply);
			}).match(patNeg, m -> {
				return i(m[0]).map(add::inverse);
			}).match(patMul, m -> {
				Node u = m[0];
				Opt<Node> vs = i(m[1]);
				Node dudx = d(u);
				return vs.concatMap(v -> i(mul.apply(v, dudx)).map(ivdu -> add.apply(mul.apply(u, v), add.inverse(ivdu))));
			}).match(patInv, m -> {
				return is_x(m[0]) ? Opt.of(patLn.subst(x)) : null;
			}).match(patExp, m -> {
				return is_x(m[0]) ? Opt.of(node) : null;
			}).match(patSin, m -> {
				return is_x(m[0]) ? Opt.of(add.inverse(patCos.subst(x))) : null;
			}).match(patCos, m -> {
				return is_x(m[0]) ? Opt.of(patSin.subst(x)) : null;
			}).applyIf(Node.class, n -> {
				if (is_x(node))
					return Opt.of(mul.apply(mul.inverse(Int.of(2)), mul.apply(x, x)));
				else if (node instanceof Int)
					return Opt.of(mul.apply(node, x));
				else
					return Opt.none();
			}).nonNullResult();
		}

		private boolean isContains_x(Node node) {
			Tree tree = Tree.decompose(node);
			return tree != null //
					? isContains_x(tree.getLeft()) || isContains_x(tree.getRight()) //
					: is_x(node);
		}

		private boolean is_x(Node node) {
			return node.compareTo(x) == 0;
		}
	}

	private Node intOf(Node n) {
		int i = ((Int) n).number;
		return i < 0 ? mul.inverse(Int.of(-i)) : n;
	}

	private Pattern patAdd = Suite.pattern(".0 + .1");
	private Pattern patNeg = Suite.pattern("neg .0");
	private Pattern patMul = Suite.pattern(".0 * .1");
	private Pattern patInv = Suite.pattern("inv .0");
	private Pattern patPow = Suite.pattern(".0^.1");
	private Pattern patExp = Suite.pattern("exp .0");
	private Pattern patLn = Suite.pattern("ln .0");
	private Pattern patSin = Suite.pattern("sin .0");
	private Pattern patCos = Suite.pattern("cos .0");

	private Group add = new Group(null, TermOp.PLUS__, N0, patNeg);
	private Group mul = new Group(add, TermOp.MULT__, N1, patInv);

	private class Group implements Fun2<Node, Node, Node> {
		private Group group0;
		private Operator operator;
		private Node e;
		private Pattern patInverse;

		private Group(Group group0, Operator operator, Node e, Pattern patInverse) {
			this.group0 = group0;
			this.operator = operator;
			this.e = e;
			this.patInverse = patInverse;
		}

		private Node recompose(Node x, Streamlet<Node> nodes0) {
			List<Node> list = new ArrayList<>();
			int xn = 0;
			Node constant = e;
			Node[] m;
			Node n;

			for (Node child : nodes0)
				if (child instanceof Int)
					constant = apply(child, constant);
				else if ((m = patNeg.match(child)) != null && (n = m[0]) instanceof Int)
					constant = apply(Int.of(-((Int) n).number), constant);
				else if (child.compareTo(x) == 0)
					xn++;
				else
					list.add(child);

			for (int i = 0; i < xn; i++)
				list.add(x);

			if (e != constant)
				list.add(intOf(constant));

			Streamlet<Node> nodes1 = Read.from(list);
			Node node = nodes1.first();

			if (node != null)
				for (Node node1 : nodes1.drop(1))
					node = apply(node1, node);
			else
				node = e;

			return node;
		}

		public Node apply(Node a, Node b) {
			Tree tree = Tree.of(operator, a, b);
			Node e0 = group0 != null ? group0.e : null;
			if (a == e0 || b == e0)
				return e0;
			else if (a == e)
				return b;
			else if (b == e)
				return a;
			else if (a instanceof Int && b instanceof Int)
				return Int.of(TreeUtil.evaluate(tree));
			else
				return tree;
		}

		public Node inverse(Node n) { // TODO for multiplication group, inv inv 0 is NaN
			Node[] m;
			if (n == e)
				return e;
			else if ((m = patInverse.match(n)) != null)
				return m[0];
			else
				return patInverse.subst(n);
		}
	}

}
