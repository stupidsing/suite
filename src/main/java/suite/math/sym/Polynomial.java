package suite.math.sym;

import java.util.function.Predicate;

import suite.BindArrayUtil.Pattern;
import suite.adt.Opt;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.math.sym.Express.OpGroup;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Polynomial<N> {

	private Express ex = new Express();

	private N n0;
	private N n1;
	private Predicate<Node> is_x;
	private Fun2<N, N, N> add_;
	private Iterate<N> neg_;
	private Fun2<N, N, N> mul_;
	private Iterate<N> inv_;
	private Fun<Node, Opt<N>> parse_;
	private Fun<N, Node> format_;

	public Polynomial( //
			Ring<N> ring0, //
			Iterate<N> inv, //
			Predicate<Node> is_x, //
			Fun<Node, Opt<N>> parse_, //
			Fun<N, Node> format_) {
		this.n0 = ring0.n0;
		this.n1 = ring0.n1;
		this.is_x = is_x;
		this.add_ = ring0.add;
		this.neg_ = ring0.neg;
		this.mul_ = ring0.mul;
		this.inv_ = inv;
		this.parse_ = parse_;
		this.format_ = format_;

		p0 = new Poly();
		p1 = new Poly(0, n1);
		px = new Poly(1, n1);
		ring = new Ring<>(p0, p1, this::add, this::neg, this::mul);
	}

	public Opt<Poly> parse(Node node) { // polynomialize
		Polynomial<N> py = Polynomial.this;

		return new Object() {
			private Opt<Poly> poly(Node node) {
				return new SwitchNode<Opt<Poly>>(node //
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
					return parse_.apply(a).map(i -> new Poly(0, i));
				}).nonNullResult();
			}

			private Opt<Poly> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return poly(a).map(pair -> { // TODO assummed a != 0 or b != 0
						Poly r = p1;
						for (char ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Poly> inv1(Opt<Poly> opt) {
				return opt.concatMap(py::inv);
			}
		}.poly(node);
	}

	private Opt<Poly> parseFraction(Node node) {
		Fractional<Poly> fractional = new Fractional<>( //
				ring, //
				a -> 0 < a.size(), //
				Polynomial.this::divMod, //
				node_ -> {
					if (node_ == n0)
						return Opt.of(p0);
					else if (is_x.test(node_))
						return Opt.of(px);
					else
						return parse_.apply(node_).map(n -> new Poly(0, n));
				}, //
				this::format);

		Iterate<Poly> sim = p -> new Poly(p.streamlet());

		return fractional //
				.fractionalize(node) //
				.concatMap(pair -> pair.map((n0, d0) -> {
					Poly n1 = sim.apply(n0);
					Poly d1 = sim.apply(d0);
					if (d1.size() == 1 && d1.get(0) == n1)
						return Opt.of(n1);
					else
						return div(n1, d1, 9);
				}));
	}

	public Node format(Poly poly) {
		Express ex = new Express();
		OpGroup add = ex.add;
		OpGroup mul = ex.mul;

		Int_Obj<Node> powerFun = p -> {
			Node power = mul.identity();
			for (int i = 0; i < p; i++)
				power = mul.apply(format_.apply(n1), power);
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

	public Poly p0;
	public Poly p1;
	public Poly px;
	public Ring<Poly> ring;

	public Opt<Poly> inv(Poly a) {
		return div(p1, a, 9);
	}

	// Euclidean
	// n / d = ((n - d * f) / (d * f) + 1) * f
	public Opt<Poly> div(Poly num, Poly denom, int depth) {
		if (num.size() <= 0)
			return Opt.of(num);
		else if (0 < depth) {
			Pair<Poly, Poly> divMod = divMod(num, denom);
			Poly f = divMod.t0; // divIntegral(num, denom);
			Poly df = mul(denom, f);
			Poly ndf = divMod.t1; // add(num, neg(df));
			return div(ndf, df, depth - 1).map(r -> mul(add(r, p1), f));
		} else
			return Opt.none();
	}

	public Pair<Poly, Poly> divMod(Poly n, Poly d) {
		if (0 < n.size()) {
			Fixie3<Integer, N, Poly> n_ = n.decons();
			Fixie3<Integer, N, Poly> d_ = d.decons();
			Poly div = new Poly(n_.get0() - d_.get0(), mul_.apply(n_.get1(), inv_.apply(d_.get1())));
			Poly mod = add(n_.get2(), neg(mul(div, d_.get2())));
			return Pair.of(div, mod);
		} else
			return Pair.of(p0, p0);
	}

	private Poly mul(Poly a, Poly b) {
		Poly c = new Poly();
		for (IntObjPair<N> pair0 : a.streamlet())
			for (IntObjPair<N> pair1 : b.streamlet())
				c.add(pair0.t0 + pair1.t0, mul_.apply(pair0.t1, pair1.t1));
		return c;
	}

	private Poly neg(Poly a) {
		return new Poly(a.streamlet().mapValue(neg_));
	}

	private Poly add(Poly a, Poly b) {
		Poly c = new Poly();
		for (IntObjPair<N> pair : IntObjStreamlet.concat(a.streamlet(), b.streamlet()))
			c.add(pair.t0, pair.t1);
		return c;
	}

	public class Poly extends IntObjMap<N> {
		public Poly(IntObjStreamlet<N> map) {
			map.sink(this::add);
		}

		public Poly(int power, N term) {
			add(power, term);
		}

		public Poly() {
		}

		public Fixie3<Integer, N, Poly> decons() {
			int max = streamlet().keys().min((p0, p1) -> p1 - p0);
			return Fixie.of(max, get(max), new Poly(streamlet().filterKey(p -> p != max)));
		}

		private void add(int power, N term) {
			Iterate<N> i0 = t -> t != null ? t : n0;
			Iterate<N> ix = t -> t != n0 ? t : null;
			update(power, t -> ix.apply(add_.apply(i0.apply(t), term)));
		}
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;
}
