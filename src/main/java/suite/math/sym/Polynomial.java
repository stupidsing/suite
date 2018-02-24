package suite.math.sym;

import java.util.function.Predicate;

import suite.adt.Opt;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Pair;
import suite.math.sym.Sym.Ring;
import suite.node.Node;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Polynomial<N> {

	private N n0;
	private N n1;
	private Predicate<Node> is_x;
	private Fun2<N, N, N> add_;
	private Iterate<N> neg_;
	private Fun2<N, N, N> mul_;
	private Iterate<N> inv_;
	private Fun<Node, Opt<N>> parse;

	public Polynomial( //
			Ring<N> ring0, //
			Predicate<Node> is_x, //
			Iterate<N> inv, //
			Fun<Node, Opt<N>> parse) {
		this.n0 = ring0.n0;
		this.n1 = ring0.n1;
		this.is_x = is_x;
		this.add_ = ring0.add;
		this.neg_ = ring0.neg;
		this.mul_ = ring0.mul;
		this.inv_ = inv;
		this.parse = parse;

		p0 = new Poly();
		p1 = new Poly(0, n1);
		px = new Poly(1, n1);
		ring = new Ring<>(p0, p1, this::add, this::neg, this::mul);
	}

	public Opt<Poly> polyize(Node node) { // polynomialize
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
						return parse.apply(node_).map(n -> new Poly(0, n));
				});

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

	private Pair<Poly, Poly> divMod(Poly n, Poly d) {
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

		private void add(int power, N term) {
			Iterate<N> i0 = t -> t != null ? t : n0;
			Iterate<N> ix = t -> t != n0 ? t : null;
			update(power, t -> ix.apply(add_.apply(i0.apply(t), term)));
		}

		private Fixie3<Integer, N, Poly> decons() {
			int max = streamlet().keys().min((p0, p1) -> p1 - p0);
			return Fixie.of(max, get(max), new Poly(streamlet().filterKey(p -> p != max)));
		}
	}

}
