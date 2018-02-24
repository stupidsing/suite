package suite.math.sym;

import java.util.function.Predicate;

import suite.BindArrayUtil.Pattern;
import suite.adt.Opt;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.math.sym.Express.OpGroup;
import suite.math.sym.Sym.Field;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Polynomial<N> {

	private Express ex = new Express();

	private Node x;
	private Predicate<Node> is_x;
	private N n0;
	private N n1;
	private Obj_Int<N> sgn_;
	private Fun2<N, N, N> add_;
	private Iterate<N> neg_;
	private Fun2<N, N, N> mul_;
	private Iterate<N> inv_;
	private Fun<Node, Opt<N>> parse_;
	private Fun<N, Node> format_;

	public Polynomial( //
			Node x, //
			Predicate<Node> is_x, //
			Field<N> field0, //
			Obj_Int<N> sgn_, //
			Fun<Node, Opt<N>> parse_, //
			Fun<N, Node> format_) {
		this.n0 = field0.n0;
		this.n1 = field0.n1;
		this.x = x;
		this.is_x = is_x;
		this.sgn_ = sgn_;
		this.add_ = field0.add;
		this.neg_ = field0.neg;
		this.mul_ = field0.mul;
		this.inv_ = field0.inv;
		this.parse_ = parse_;
		this.format_ = format_;

		p0 = new Poly<>(this);
		p1 = polyOf(0, n1);
		px = polyOf(1, n1);
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
		Polynomial<N> py = Polynomial.this;

		return new Object() {
			private Opt<Poly<N>> poly(Node node) {
				return new SwitchNode<Opt<Poly<N>>>(node //
				).match2(patAdd, (a, b) -> {
					return poly(a).join(poly(b), py::add);
				}).match1(patNeg, a -> {
					return poly(a).map(py::neg);
				}).match2(patMul, (a, b) -> {
					return poly(a).join(poly(b), py::mul);
				}).match1(patInv, a -> {
					return inv1(poly(a));
				}).match2(patPow, (a, b) -> {
					return b instanceof Int ? pow(a, ((Int) b).number) : Opt.none();
				}).applyIf(Node.class, a -> {
					if (is_x.test(a))
						return Opt.of(px);
					else
						return parse_.apply(a).map(n -> polyOf(0, n));
				}).nonNullResult();
			}

			private Opt<Poly<N>> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return poly(a).map(pair -> { // TODO assummed a != 0 or b != 0
						Poly<N> r = p1;
						for (char ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Poly<N>> inv1(Opt<Poly<N>> opt) {
				return opt.concatMap(py::inv);
			}
		}.poly(node);
	}

	private Opt<Poly<N>> parseFraction(Node node) {
		Polynomial<N> py = Polynomial.this;

		Fractional<Poly<N>> fractional = new Fractional<>( //
				ring, //
				py::sign, //
				py::divMod, //
				node_ -> {
					if (node_ == n0)
						return Opt.of(p0);
					else if (is_x.test(node_))
						return Opt.of(px);
					else
						return parse_.apply(node_).map(n -> polyOf(0, n));
				}, //
				this::format);

		Iterate<Poly<N>> sim = p -> polyOf(p.streamlet());

		return fractional //
				.fractionalize(node) //
				.concatMap(pair -> pair.map((n0, d0) -> {
					Poly<N> n1 = sim.apply(n0);
					Poly<N> d1 = sim.apply(d0);
					if (d1.size() == 1 && d1.get(0) == n1)
						return Opt.of(n1);
					else
						return div(n1, d1, 9);
				}));
	}

	public Node format(Poly<N> poly) {
		Express ex = new Express();
		OpGroup add = ex.add;
		OpGroup mul = ex.mul;

		Int_Obj<Node> powerFun = p -> {
			Node power = mul.identity();
			for (int i = 0; i < p; i++)
				power = mul.apply(x, power);
			return power;
		};

		Node sum = format_.apply(n0);

		for (IntObjPair<N> pair : poly.streamlet().sortByKey(Integer::compare)) {
			int p = pair.t0;
			Node power = p < 0 ? mul.inverse(powerFun.apply(-p)) : powerFun.apply(p);
			sum = add.apply(mul.apply(format_.apply(pair.t1), power), sum);
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
			Pair<Poly<N>, Poly<N>> divMod = divMod(num, denom);
			Poly<N> f = divMod.t0; // divIntegral(num, denom);
			Poly<N> df = mul(denom, f);
			Poly<N> ndf = divMod.t1; // add(num, neg(df));
			return div(ndf, df, depth - 1).map(r -> mul(add(r, p1), f));
		} else
			return Opt.none();
	}

	private Pair<Poly<N>, Poly<N>> divMod(Poly<N> n, Poly<N> d) {
		if (0 < n.size()) {
			Fixie3<Integer, N, Poly<N>> n_ = n.decons();
			Fixie3<Integer, N, Poly<N>> d_ = d.decons();
			Poly<N> div = polyOf(n_.get0() - d_.get0(), mul_.apply(n_.get1(), inv_.apply(d_.get1())));
			Poly<N> mod = add(n_.get2(), neg(mul(div, d_.get2())));
			return Pair.of(div, mod);
		} else
			return Pair.of(p0, p0);
	}

	private Poly<N> mul(Poly<N> a, Poly<N> b) {
		Poly<N> c = new Poly<>(this);
		for (IntObjPair<N> pair0 : a.streamlet())
			for (IntObjPair<N> pair1 : b.streamlet())
				c.add(pair0.t0 + pair1.t0, mul_.apply(pair0.t1, pair1.t1));
		return c;
	}

	private Poly<N> neg(Poly<N> a) {
		return polyOf(a.streamlet().mapValue(neg_));
	}

	private Poly<N> add(Poly<N> a, Poly<N> b) {
		Poly<N> c = new Poly<>(this);
		for (IntObjPair<N> pair : IntObjStreamlet.concat(a.streamlet(), b.streamlet()))
			c.add(pair.t0, pair.t1);
		return c;
	}

	public int sign(Poly<N> a) {
		return 0 < a.size() ? sgn_.apply(a.decons().get1()) : 0;
	}

	private Poly<N> polyOf(IntObjStreamlet<N> map) {
		return new Poly<>(this, map);
	}

	public Poly<N> polyOf(int power, N term) {
		Poly<N> poly = new Poly<>(this);
		poly.add(power, term);
		return poly;
	}

	public static class Poly<N> extends IntObjMap<N> {
		private Polynomial<N> py;

		private Poly(Polynomial<N> py, IntObjStreamlet<N> map) {
			this(py);
			map.sink(this::add);
		}

		private Poly(Polynomial<N> py) {
			this.py = py;
		}

		public Fixie3<Integer, N, Poly<N>> decons() {
			int max = streamlet().keys().min((p0, p1) -> p1 - p0);
			return Fixie.of(max, get(max), new Poly<>(py, streamlet().filterKey(p -> p != max)));
		}

		private void add(int power, N term) {
			N n0 = py.n0;
			Iterate<N> i0 = t -> t != null ? t : n0;
			Iterate<N> ix = t -> t != n0 ? t : null;
			update(power, t -> ix.apply(py.add_.apply(i0.apply(t), term)));
		}
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;
}
