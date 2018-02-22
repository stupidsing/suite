package suite.math.sym;

import java.util.ArrayList;
import java.util.List;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.Opt;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaInstance;
import suite.math.sym.Express.OpGroup;
import suite.math.sym.Sym.Field;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil2.Fun2;
import suite.util.List_;

public class Symbolic {

	private Express ex = new Express();
	private FunFactory f = new FunFactory();
	private Int n0 = ex.n0;
	private Int n1 = ex.n1;

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
		).match2(patAdd, (a, b) -> {
			return f.bi("+", fun.apply(a), fun.apply(b));
		}).match1(patNeg, a -> {
			return f.bi("-", f.double_(0d), fun.apply(a));
		}).match2(patMul, (a, b) -> {
			return f.bi("*", fun.apply(a), fun.apply(b));
		}).match1(patInv, a -> {
			return f.bi("/", f.double_(1d), fun.apply(a));
		}).match2(patPow, (a, b) -> {
			return f.invokeStatic(Math.class, "pow", fun.apply(a), fun.apply(b));
		}).match1(patExp, a -> {
			return f.invokeStatic(Math.class, "exp", fun.apply(a));
		}).match1(patLn_, a -> {
			return f.invokeStatic(Math.class, "log", fun.apply(a));
		}).match1(patSin, a -> {
			return f.invokeStatic(Math.class, "sin", fun.apply(a));
		}).match1(patCos, a -> {
			return f.invokeStatic(Math.class, "cos", fun.apply(a));
		}).applyIf(Int.class, i -> {
			return f.double_(i.number);
		}).nonNullResult();
	}

	public Node d(Node node0, Node x) {
		Rewrite rewrite = new Rewrite(x);
		return Opt.of(node0).map(rewrite::rewrite).map(rewrite::d).map(rewrite::simplify).get();
	}

	public Node i(Node node0, Node x) {
		Rewrite rewrite = new Rewrite(x);
		return Opt.of(node0).map(rewrite::rewrite).concatMap(rewrite::i).map(rewrite::simplify).get();
	}

	public Node simplify(Node node, Node... xs) {
		return simplify(node, xs, 0);
	}

	private Node simplify(Node node, Node[] xs, int i) {
		Opt<Node> opt;
		if (i < xs.length)
			opt = new Rewrite(xs[i]).polyize(node, coeff -> simplify(coeff, xs, i + 1));
		else
			opt = rational(node);
		return opt.or(() -> node);
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
			).match2(".0 - .1", (a, b) -> {
				return a != n0 ? add(rewrite(a), neg(rewrite(b))) : null;
			}).match2(".0 / .1", (a, b) -> {
				return a != n1 ? mul(rewrite(a), inv(rewrite(b))) : null;
			}).match2(patPow, (a, b) -> {
				return patExp.subst(patLn_.subst(rewrite(a)), rewrite(b));
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

		private Node d(Node node) { // differentiation
			return new SwitchNode<Node>(node //
			).match2(patAdd, (u, v) -> {
				return add(d(u), d(v));
			}).match1(patNeg, u -> {
				return neg(d(u));
			}).match2(patMul, (u, v) -> {
				return add(mul(u, d(v)), mul(v, d(u)));
			}).match1(patInv, u -> {
				return mul(inv(mul(u, u)), neg(d(u)));
			}).match1(patExp, u -> {
				return mul(patExp.subst(u), d(u));
			}).match1(patLn_, u -> {
				return mul(inv(u), d(u));
			}).match1(patSin, u -> {
				return mul(patCos.subst(u), d(u));
			}).match1(patCos, u -> {
				return mul(neg(patSin.subst(u)), d(u));
			}).applyIf(Int.class, n -> {
				return n0;
			}).applyIf(Node.class, n -> {
				return is_x(node) ? n1 : null;
			}).nonNullResult();
		}

		private Opt<Node> i(Node node) { // integration
			return new SwitchNode<Opt<Node>>(node //
			).match2(patAdd, (u, v) -> {
				Opt<Node> iudxs = i(u);
				Opt<Node> ivdxs = i(v);
				return iudxs.join(ivdxs, add::apply);
			}).match1(patNeg, u -> {
				return i(u).map(add::inverse);
			}).match2(patMul, (m0, m1) -> {
				Node u = m0;
				Opt<Node> vs = i(m1);
				Node dudx = d(u);
				return vs.concatMap(v -> i(mul(v, dudx)).map(ivdu -> add(mul(u, v), neg(ivdu))));
			}).match1(patInv, u -> {
				return is_x(u) ? Opt.of(patLn_.subst(x)) : null;
			}).match1(patExp, u -> {
				return is_x(u) ? Opt.of(node) : null;
			}).match1(patSin, u -> {
				return is_x(u) ? Opt.of(neg(patCos.subst(x))) : null;
			}).match1(patCos, u -> {
				return is_x(u) ? Opt.of(patSin.subst(x)) : null;
			}).applyIf(Node.class, n -> {
				if (is_x(node))
					return Opt.of(mul(inv(Int.of(2)), mul(x, x)));
				else if (node instanceof Int)
					return Opt.of(mul(node, x));
				else
					return Opt.none();
			}).nonNullResult();
		}

		private Node sumOfProducts(Node node) {
			class Recurse {
				private Streamlet<Node> pos(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match2(patMul, (a, b) -> {
						return Streamlet.concat(pos(a), pos(b));
					}).match1(patInv, a -> {
						return pos(a).map(mul::inverse);
					}).match2(patPow, (a, b) -> {
						if (b instanceof Int) {
							List<Node> pos = pos(a).toList();
							int power = ((Int) b).number;

							Int_Obj<Streamlet<Node>> f = power_ -> {
								List<Node> n = new ArrayList<>();
								for (char ch : Integer.toBinaryString(power_).toCharArray()) {
									n = List_.concat(n, n);
									n = ch != '0' ? List_.concat(n, pos) : n;
								}
								return Read.from(n);
							};

							if (power < 0)
								return f.apply(-power).map(mul::inverse);
							else // TODO assumed a != 0 || power != 0
								return f.apply(power);
						} else
							return pos(a).join2(sop(b)).map(patPow::subst);
					}).match1(patExp, a -> {
						return sop(a).map(patExp::subst);
					}).applyIf(Node.class, n -> {
						return node_ == n1 ? Read.empty() : Read.each(node_);
					}).nonNullResult();
				}

				private Streamlet<Node> sop(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match2(patAdd, (a, b) -> {
						return Streamlet.concat(sop(a), sop(b));
					}).match1(patNeg, a -> {
						return sop(a).map(add::inverse);
					}).match2(patMul, (a, b) -> {
						return sop(a).join2(sop(b)).map(mul::apply).map(this::productOfSums);
					}).match2(patPow, (a, b) -> {
						return sop(productOfSums(node_));
					}).match1(patLn_, a -> {
						return pos(a).map(patLn_::subst);
					}).match2("sin (.0 + .1)", (a, b) -> {
						return Read.each( //
								mul.recompose(x, Read.each(patSin.subst(a), patCos.subst(b))), //
								mul.recompose(x, Read.each(patCos.subst(a), patSin.subst(b))));
					}).match2("cos (.0 + .1)", (a, b) -> {
						return Read.each( //
								mul.recompose(x, Read.each(patCos.subst(a), patCos.subst(b))), //
								mul.recompose(x, Read.each(neg(patSin.subst(a)), patSin.subst(b))));
					}).applyIf(Node.class, n -> {
						return node_ == n0 ? Read.empty() : Read.each(node_);
					}).nonNullResult();
				}

				private Node productOfSums(Node node) {
					return mul.recompose(x, pos(node));
				}

				private Node sumOfProducts(Node node) {
					return add.recompose(x, sop(node));
				}
			}

			return new Recurse().sumOfProducts(node);
		}

		private Node simplify(Node node) {
			return polyize(node, coeff -> rational(coeff).or(() -> coeff)).or(() -> sumOfProducts(node));
		}

		private Opt<Node> polyize(Node node, Fun<Node, Node> coefficientFun) { // polynomialize
			Int_Obj<Node> powerFun = p -> {
				Node power = n1;
				for (int i = 0; i < p; i++)
					power = mul(x, power);
				return power;
			};

			Ring<Node> ring = Boolean.FALSE ? ex.field
					: new Ring<>( //
							n0, //
							n1, //
							(a, b) -> coefficientFun.apply(add(a, b)), //
							add::inverse, //
							(a, b) -> coefficientFun.apply(mul(a, b)));

			Opt<Polynomial<Node>.Poly> opt;

			if (Boolean.FALSE)
				opt = new Polynomial<>( //
						ring, //
						this::is_x, //
						mul::inverse, //
						n -> !isContains_x(n) ? Opt.of(n) : Opt.none()).polyize(node);
			else {
				Field<Node> nf = ex.field;

				class PN extends Polynomial<Node> {
					PN() {
						super(nf, Rewrite.this::is_x, nf.inv, Opt::of);
					}
				}

				Polynomial<Node> pn = new PN();
				Ring<PN.Poly> pr = pn.ring;

				opt = new Object() {
					Opt<PN.Poly> poly(Node node) {
						return new SwitchNode<Opt<PN.Poly>>(node //
						).match2(patAdd, (a, b) -> {
							return poly(a).join(poly(b), pr.add);
						}).match1(patNeg, a -> {
							return poly(a).map(pr.neg);
						}).match2(patMul, (a, b) -> {
							return poly(a).join(poly(b), pr.mul);
						}).match1(patInv, a -> {
							return inv1(poly(a));
						}).match2(patPow, (a, b) -> {
							return b instanceof Int ? pow(a, ((Int) b).number) : Opt.none();
						}).applyIf(Node.class, n -> {
							if (n == nf.n0)
								return Opt.of(pn.p0);
							else if (is_x(n))
								return Opt.of(pn.px);
							else if (!isContains_x(n))
								return Opt.of(pn.new Poly(0, n));
							else
								return Opt.none();
						}).nonNullResult();
					}

					private Opt<PN.Poly> pow(Node a, int power) {
						if (power < 0)
							return inv1(pow(a, -power));
						else // TODO assumed m0 != 0 or power != 0
							return poly(a).map(p -> {
								PN.Poly r = pn.p1;
								for (char ch : Integer.toBinaryString(power).toCharArray()) {
									r = pr.mul.apply(r, r);
									r = ch != '0' ? pr.mul.apply(p, r) : r;
								}
								return r;
							});
					}

					private Opt<PN.Poly> inv1(Opt<PN.Poly> opt) {
						return opt.concatMap(pn::inv);
					}
				}.poly(node);
			}

			return opt.map(map -> {
				Node sum = n0;
				for (IntObjPair<Node> pair : map.streamlet().sortByKey(Integer::compare)) {
					int p = pair.t0;
					Node power = p < 0 ? inv(powerFun.apply(-p)) : powerFun.apply(p);
					sum = add(mul(coefficientFun.apply(pair.t1), power), sum);
				}
				return sum;
			});
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

	private Opt<Node> rational(Node node) {
		Fun2<Integer, Integer, Node> nf0 = (n, d) -> mul(Int.of(n), inv(Int.of(d)));
		Fun2<Integer, Integer, Node> nf1 = (n, d) -> 0 <= n ? nf0.apply(n, d) : neg(nf0.apply(-n, d));

		return Fractional //
				.ofIntegral() //
				.fractionalize(node) //
				.map(pair -> pair.map(nf1));
	}

	private Node add(Node a, Node b) {
		return add.apply(a, b);
	}

	private Node neg(Node a) {
		return add.inverse(a);
	}

	private Node mul(Node a, Node b) {
		return mul.apply(a, b);
	}

	private Node inv(Node a) {
		return mul.inverse(a);
	}

	private Node intOf(Node n) {
		return ex.intOf(n);
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;
	private Pattern patExp = ex.patExp;
	private Pattern patLn_ = ex.patLn_;
	private Pattern patSin = ex.patSin;
	private Pattern patCos = ex.patCos;

	private OpGroup add = ex.add;
	private OpGroup mul = ex.mul;

}
