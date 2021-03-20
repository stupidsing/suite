package suite.math.sym;

import java.util.function.Predicate;

import primal.adt.Opt;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs2.Fun2;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.IntPrim.Obj_Int;
import suite.BindArrayUtil.Pattern;
import suite.math.sym.Polynomial.Poly;
import suite.math.sym.Sym.Field;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;

public class DivisiblePolynomial<N> {

	private Polynomial<N> py;

	private Express ex = new Express();

	private Node x;
	private Predicate<Node> is_x;
	private N n0;
	private N n1;
	private Obj_Int<N> sgn_;
	private Fun2<N, N, N> mul_;
	private Iterate<N> inv_;
	private Fun<Node, Opt<N>> parse_;
	private Fun<N, Node> format_;

	public DivisiblePolynomial( //
			Node x, //
			Predicate<Node> is_x, //
			Field<N> field0, //
			Obj_Int<N> sgn_, //
			Fun<Node, Opt<N>> parse_, //
			Fun<N, Node> format_) {
		this.py = new Polynomial<>(x, is_x, field0, sgn_, parse_, format_);
		this.n0 = field0.n0;
		this.n1 = field0.n1;
		this.x = x;
		this.is_x = is_x;
		this.sgn_ = sgn_;
		this.mul_ = field0.mul;
		this.inv_ = field0.inv;
		this.parse_ = parse_;
		this.format_ = format_;

		p0 = py.polyOf();
		p1 = py.polyOf(0, n1);
		px = py.polyOf(1, n1);
		ring = new Ring<>(p0, p1, this::add, this::neg, this::mul);
	}

	public Fractional<Poly<N>> fractional() {
		return new Fractional<>( //
				this.ring, //
				this::sign, //
				this::divMod, //
				this::parse, //
				this::format);
	}

	public Opt<Poly<N>> parse(Node node) { // polynomialize
		DivisiblePolynomial<N> dpy = DivisiblePolynomial.this;

		return new Object() {
			private Opt<Poly<N>> poly(Node node) {
				return new SwitchNode<Opt<Poly<N>>>(node //
				).match(patAdd, (a, b) -> {
					return poly(a).join(poly(b), dpy::add);
				}).match(patNeg, a -> {
					return poly(a).map(dpy::neg);
				}).match(patMul, (a, b) -> {
					return poly(a).join(poly(b), dpy::mul);
				}).match(patInv, a -> {
					return inv1(poly(a));
				}).match(patPow, (a, b) -> {
					return b instanceof Int i ? pow(a, i.number) : Opt.none();
				}).applyIf(Node.class, a -> {
					if (is_x.test(a))
						return Opt.of(px);
					else
						return parse_.apply(a).map(n -> py.polyOf(0, n));
				}).nonNullResult();
			}

			private Opt<Poly<N>> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return poly(a).map(pair -> { // TODO assumed a != 0 or b != 0
						Poly<N> r = p1;
						for (var ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Poly<N>> inv1(Opt<Poly<N>> opt) {
				return opt.concatMap(dpy::inv);
			}
		}.poly(node);
	}

	public Node format(Poly<N> poly) {
		var ex = new Express();
		var add = ex.add;
		var mul = ex.mul;

		Int_Obj<Node> powerFun = p -> {
			var power = mul.identity();
			for (var i = 0; i < p; i++)
				power = mul.apply(x, power);
			return power;
		};

		var sum = format_.apply(n0);

		for (var pair : ReadInt.from2(poly).sortByKey(Integer::compare)) {
			var p = pair.k;
			var power = p < 0 ? mul.inverse(powerFun.apply(-p)) : powerFun.apply(p);
			sum = add.apply(mul.apply(format_.apply(pair.v), power), sum);
		}

		return sum;
	}

	public Poly<N> p0;
	public Poly<N> p1;
	public Poly<N> px;
	public Ring<Poly<N>> ring;

	public Opt<Poly<N>> inv(Poly<N> a) {
		return div(p1, a, 9);
	}

	// Euclidean
	// n / d = ((n - d * f) / (d * f) + 1) * f
	private Opt<Poly<N>> div(Poly<N> num, Poly<N> denom, int depth) {
		if (num.size() <= 0)
			return Opt.of(num);
		else if (0 < depth) {
			var divMod = divMod(num, denom);
			var f = divMod.k; // divIntegral(num, denom);
			var df = mul(denom, f);
			var ndf = divMod.v; // add(num, neg(df));
			return div(ndf, df, depth - 1).map(r -> mul(add(r, p1), f));
		} else
			return Opt.none();
	}

	private Pair<Poly<N>, Poly<N>> divMod(Poly<N> n, Poly<N> d) {
		if (0 < n.size()) {
			var n_ = n.decons();
			var d_ = d.decons();
			var div = py.polyOf(n_.get0() - d_.get0(), mul_.apply(n_.get1(), inv_.apply(d_.get1())));
			var mod = add(n_.get2(), neg(mul(div, d_.get2())));
			return Pair.of(div, mod);
		} else
			return Pair.of(p0, p0);
	}

	private Poly<N> mul(Poly<N> a, Poly<N> b) {
		return py.ring.mul.apply(a, b);
	}

	private Poly<N> neg(Poly<N> a) {
		return py.ring.neg.apply(a);
	}

	private Poly<N> add(Poly<N> a, Poly<N> b) {
		return py.ring.add.apply(a, b);
	}

	public int sign(Poly<N> a) {
		return 0 < a.size() ? sgn_.apply(a.decons().get1()) : 0;
	}

	public Poly<N> polyOf(int power, N term) {
		return py.polyOf(power, term);
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;
}
