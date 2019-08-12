package suite.math.sym;

import java.util.ArrayList;
import java.util.List;

import primal.Verbs.Concat;
import primal.adt.Opt;
import primal.fp.Funs.Fun;
import primal.fp.Funs2.Fun2;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.Dbl_Dbl;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.IntPrim.Obj_Int;
import primal.streamlet.Streamlet;
import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.lambda.LambdaInstance;
import suite.math.sym.Express.OpGroup;
import suite.math.sym.Fractional.Fract;
import suite.math.sym.Polynomial.Poly;
import suite.math.sym.Sym.Field;
import suite.math.sym.Sym.Ring;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.streamlet.Read;

public class Symbolic {

	private Express ex = new Express();
	private FunFactory f = new FunFactory();
	private Node n0 = ex.n0;
	private Node n1 = ex.n1;

	public class PolynomializeException extends RuntimeException {
		private static final long serialVersionUID = 1l;
	}

	public Dbl_Dbl fun(Node fn, Node node0) {
		return LambdaInstance.of(Dbl_Dbl.class, x -> new Object() {
			private FunExpr fun(Node n) {
				return n == node0 ? x : m(n, this::fun);
			}
		}.fun(fn)).newFun();
	}

	public Obj_Dbl<float[]> fun(Node fn, Node[] vars) {
		@SuppressWarnings("unchecked")
		Obj_Dbl<float[]> fun = LambdaInstance.of(Obj_Dbl.class, x -> new Object() {
			private FunExpr fun(Node n) {
				for (var i = 0; i < vars.length; i++)
					if (n == vars[i])
						return x.index(f.int_(i));
				return m(n, this::fun);
			}
		}.fun(fn)).newFun();

		return fun;
	}

	private FunExpr m(Node n, Fun<Node, FunExpr> fun) {
		return new SwitchNode<FunExpr>(n //
		).match(patAdd, (a, b) -> {
			return f.bi("+", fun.apply(a), fun.apply(b));
		}).match(patNeg, a -> {
			return f.bi("-", f.double_(0d), fun.apply(a));
		}).match(patMul, (a, b) -> {
			return f.bi("*", fun.apply(a), fun.apply(b));
		}).match(patInv, a -> {
			return f.bi("/", f.double_(1d), fun.apply(a));
		}).match(patPow, (a, b) -> {
			return f.invokeStatic(Math.class, "pow", fun.apply(a), fun.apply(b));
		}).match(patExp, a -> {
			return f.invokeStatic(Math.class, "exp", fun.apply(a));
		}).match(patLn_, a -> {
			return f.invokeStatic(Math.class, "log", fun.apply(a));
		}).match(patSin, a -> {
			return f.invokeStatic(Math.class, "sin", fun.apply(a));
		}).match(patCos, a -> {
			return f.invokeStatic(Math.class, "cos", fun.apply(a));
		}).applyIf(Int.class, i -> {
			return f.double_(i.number);
		}).nonNullResult();
	}

	public Node d(Node node0, Node x) {
		var rewrite = new Rewrite(x);
		return Opt.of(node0).map(rewrite::rewrite).map(rewrite::d).map(rewrite::simplify).get();
	}

	public Node i(Node node0, Node x) {
		var rewrite = new Rewrite(x);
		return Opt.of(node0).map(rewrite::rewrite).concatMap(rewrite::i).map(rewrite::simplify).get();
	}

	public static class Fieldo<N> {
		public final Field<N> field;
		public final Obj_Int<N> sgn;
		public final Fun<Node, Opt<N>> parse;
		public final Fun<N, Node> format;

		public static <N> Fieldo<Fract<N>> ofFractional(Fractional<N> p) {
			return new Fieldo<>(p.field, p::sign, p::parse, p::format);
		}

		public Fieldo(Field<N> field, Obj_Int<N> sgn, Fun<Node, Opt<N>> parse, Fun<N, Node> format) {
			this.field = field;
			this.sgn = sgn;
			this.parse = parse;
			this.format = format;
		}

		public Fieldo<Fract<Poly<N>>> fp(Rewrite rewrite) {
			return Fieldo.ofFractional(new DivisiblePolynomial<>(rewrite.x, rewrite::is_x, field, sgn, parse, format).fractional());
		}

		public Opt<Node> pf(Node node) {
			return parse.apply(node).map(format);
		}
	}

	public static class Ringo<N> {
		public final Ring<N> ring;
		public final Obj_Int<N> sgn;
		public final Fun<Node, Opt<N>> parse;
		public final Fun<N, Node> format;

		public static <N> Ringo<Fract<N>> ofFractional(Fractional<N> p) {
			return new Ringo<>(p.field, p::sign, p::parse, p::format);
		}

		public static <N> Ringo<Poly<N>> ofPolynomial(Polynomial<N> p) {
			return new Ringo<>(p.ring, p::sign, p::parse, p::format);
		}

		public Ringo(Ring<N> ring, Obj_Int<N> sgn, Fun<Node, Opt<N>> parse, Fun<N, Node> format) {
			this.ring = ring;
			this.sgn = sgn;
			this.parse = parse;
			this.format = format;
		}

		public Ringo<Poly<N>> poly(Rewrite rewrite) {
			return ofPolynomial(new Polynomial<>(rewrite.x, rewrite::is_x, ring, sgn, parse, format));
		}

		private Opt<Node> pf(Node node) {
			return parse.apply(node).map(format);
		}
	}

	public Opt<Node> polyize(Node node, Atom... vars) {
		return Read //
				.from(vars) //
				.<Ringo<?>> fold(Ringo.ofFractional(Fractional.ofIntegral()), (r, var) -> r.poly(new Rewrite(var))) //
				.pf(node);
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "unused", })
	private Opt<Node> polyize0(Node node, Atom... vars) {
		var fractional_ = Read //
				.from(vars) //
				.<Fractional<?>> fold(Fractional.ofIntegral(), (fr, var) -> divPoly(new Rewrite(var), fr).fractional());

		return fractional_.parse(node).map(o -> fractional_.format((Fract) o));
	}

	public Opt<Node> polyize_xyn(Node node) {
		var rewrite_x = new Rewrite(Atom.of("x"));
		var rewrite_y = new Rewrite(Atom.of("y"));
		return Fieldo.ofFractional(Fractional.ofIntegral()).fp(rewrite_y).fp(rewrite_x).pf(node);
	}

	private static <I> DivisiblePolynomial<Fract<I>> divPoly(Rewrite rewrite, Fractional<I> fractional) {
		return new DivisiblePolynomial<>( //
				rewrite.x, //
				rewrite::is_x, //
				fractional.field, //
				fractional::sign, //
				fractional::parse, //
				fractional::format);
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
		return opt.or(() -> node).get();
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
			).match(".0 - .1", (a, b) -> {
				return a != n0 ? add(rewrite(a), neg(rewrite(b))) : null;
			}).match(".0 / .1", (a, b) -> {
				return a != n1 ? mul(rewrite(a), inv(rewrite(b))) : null;
			}).match(patPow, (a, b) -> {
				return patExp.subst(patLn_.subst(rewrite(a)), rewrite(b));
			}).applyIf(Int.class, i -> {
				return intOf(i);
			}).matchArray(".0 .1", m -> {
				return Suite.pattern(".0 .1").subst(m[0], rewrite(m[1]));
			}).applyTree((op, l, r) -> {
				return Tree.of(op, rewrite(l), rewrite(r));
			}).applyIf(Node.class, n -> {
				return n;
			}).nonNullResult();
		}

		private Node d(Node node) { // differentiation
			return new SwitchNode<Node>(node //
			).match(patAdd, (u, v) -> {
				return add(d(u), d(v));
			}).match(patNeg, u -> {
				return neg(d(u));
			}).match(patMul, (u, v) -> {
				return add(mul(u, d(v)), mul(v, d(u)));
			}).match(patInv, u -> {
				return mul(inv(mul(u, u)), neg(d(u)));
			}).match(patExp, u -> {
				return mul(patExp.subst(u), d(u));
			}).match(patLn_, u -> {
				return mul(inv(u), d(u));
			}).match(patSin, u -> {
				return mul(patCos.subst(u), d(u));
			}).match(patCos, u -> {
				return mul(neg(patSin.subst(u)), d(u));
			}).applyIf(Int.class, n -> {
				return n0;
			}).applyIf(Node.class, n -> {
				return is_x(node) ? n1 : null;
			}).nonNullResult();
		}

		private Opt<Node> i(Node node) { // integration
			return new SwitchNode<Opt<Node>>(node //
			).match(patAdd, (u, v) -> {
				var iudxs = i(u);
				var ivdxs = i(v);
				return iudxs.join(ivdxs, add::apply);
			}).match(patNeg, u -> {
				return i(u).map(add::inverse);
			}).match(patMul, (m0, m1) -> {
				var u = m0;
				var vs = i(m1);
				var dudx = d(u);
				return vs.concatMap(v -> i(mul(v, dudx)).map(ivdu -> add(mul(u, v), neg(ivdu))));
			}).match(patInv, u -> {
				return is_x(u) ? Opt.of(patLn_.subst(x)) : null;
			}).match(patExp, u -> {
				return is_x(u) ? Opt.of(node) : null;
			}).match(patSin, u -> {
				return is_x(u) ? Opt.of(neg(patCos.subst(x))) : null;
			}).match(patCos, u -> {
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
			var recurse = new Object() {
				private Streamlet<Node> pos(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match(patMul, (a, b) -> {
						return Streamlet.concat(pos(a), pos(b));
					}).match(patInv, a -> {
						return pos(a).map(mul::inverse);
					}).match(patPow, (a, b) -> {
						if (b instanceof Int) {
							var pos = pos(a).toList();
							var power = Int.num(b);

							Int_Obj<Streamlet<Node>> f = power_ -> {
								List<Node> n = new ArrayList<>();
								for (var ch : Integer.toBinaryString(power_).toCharArray()) {
									n = Concat.lists(n, n);
									n = ch != '0' ? Concat.lists(n, pos) : n;
								}
								return Read.from(n);
							};

							if (power < 0)
								return f.apply(-power).map(mul::inverse);
							else // TODO assumed a != 0 || power != 0
								return f.apply(power);
						} else
							return pos(a).join2(sop(b)).map(patPow::subst);
					}).match(patExp, a -> {
						return sop(a).map(patExp::subst);
					}).applyIf(Node.class, n -> {
						return node_ == n1 ? Read.empty() : Read.each(node_);
					}).nonNullResult();
				}

				private Streamlet<Node> sop(Node node_) {
					return new SwitchNode<Streamlet<Node>>(node_ //
					).match(patAdd, (a, b) -> {
						return Streamlet.concat(sop(a), sop(b));
					}).match(patNeg, a -> {
						return sop(a).map(add::inverse);
					}).match(patMul, (a, b) -> {
						return sop(a).join2(sop(b)).map(mul::apply).map(this::productOfSums);
					}).match(patPow, (a, b) -> {
						return sop(productOfSums(node_));
					}).match(patLn_, a -> {
						return pos(a).map(patLn_::subst);
					}).match("sin (.0 + .1)", (a, b) -> {
						return Read.each( //
								mul.recompose(x, Read.each(patSin.subst(a), patCos.subst(b))), //
								mul.recompose(x, Read.each(patCos.subst(a), patSin.subst(b))));
					}).match("cos (.0 + .1)", (a, b) -> {
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
			};

			return recurse.sumOfProducts(node);
		}

		private Node simplify(Node node) {
			return polyize(node, coeff -> rational(coeff).get(() -> coeff)).get(() -> sumOfProducts(node));
		}

		private Opt<Node> polyize(Node node, Fun<Node, Node> coefficientFun) { // polynomialize
			Int_Obj<Node> powerFun = p -> {
				var power = n1;
				for (var i = 0; i < p; i++)
					power = mul(x, power);
				return power;
			};

			return polyize_(node, coefficientFun).map(map -> {
				var sum = n0;
				for (var pair : ReadInt.from2(map).sortByKey(Integer::compare)) {
					var p = pair.k;
					var power = p < 0 ? inv(powerFun.apply(-p)) : powerFun.apply(p);
					sum = add(mul(coefficientFun.apply(pair.v), power), sum);
				}
				return sum;
			});
		}

		private Opt<Poly<Node>> polyize_(Node node, Fun<Node, Node> coefficientFun) {
			var nf = ex.field;
			Obj_Int<Node> sign = a -> a.compareTo(ex.n0);
			var dpn = new DivisiblePolynomial<Node>(x, Rewrite.this::is_x, nf, sign, Opt::of, n -> n);
			var pr = dpn.ring;

			return new Object() {
				private Opt<Poly<Node>> poly(Node node) {
					return new SwitchNode<Opt<Poly<Node>>>(node //
					).match(patAdd, (a, b) -> {
						return poly(a).join(poly(b), pr.add);
					}).match(patNeg, a -> {
						return poly(a).map(pr.neg);
					}).match(patMul, (a, b) -> {
						return poly(a).join(poly(b), pr.mul);
					}).match(patInv, a -> {
						return inv1(poly(a));
					}).match(patPow, (a, b) -> {
						return b instanceof Int ? pow(a, Int.num(b)) : Opt.none();
					}).applyIf(Node.class, n -> {
						if (n == nf.n0)
							return Opt.of(dpn.p0);
						else if (is_x(n))
							return Opt.of(dpn.px);
						else if (!isContains_x(n))
							return Opt.of(dpn.polyOf(0, n));
						else
							return Opt.none();
					}).nonNullResult();
				}

				private Opt<Poly<Node>> pow(Node a, int power) {
					if (power < 0)
						return inv1(pow(a, -power));
					else // TODO assumed m0 != 0 or power != 0
						return poly(a).map(p -> {
							var r = dpn.p1;
							for (var ch : Integer.toBinaryString(power).toCharArray()) {
								r = pr.mul.apply(r, r);
								r = ch != '0' ? pr.mul.apply(p, r) : r;
							}
							return r;
						});
				}

				private Opt<Poly<Node>> inv1(Opt<Poly<Node>> opt) {
					return opt.concatMap(dpn::inv);
				}
			}.poly(node);
		}

		private boolean isContains_x(Node node) {
			var tree = Tree.decompose(node);
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
		return Fractional.ofIntegral().fractionalize(node).map(pair -> pair.map(nf1));
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
