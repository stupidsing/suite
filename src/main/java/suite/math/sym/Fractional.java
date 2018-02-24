package suite.math.sym;

import java.util.function.Predicate;

import suite.BindArrayUtil.Pattern;
import suite.adt.Opt;
import suite.adt.pair.Pair;
import suite.math.sym.Express.OpGroup;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Fractional<I> {

	private static Express ex = new Express();

	private I n0;
	private I n1;
	private Predicate<I> isPositive;
	private Predicate<I> isZero;
	private Fun2<I, I, I> add_;
	private Iterate<I> neg_;
	private Fun2<I, I, I> mul_;
	private Fun2<I, I, Pair<I, I>> divMod_;
	private Fun<Node, Opt<I>> parse;
	private Fun<I, Node> format;

	public static Fractional<Integer> ofIntegral() {
		return new Fractional<>( //
				new Integral().ring, //
				a -> 0 < a, //
				(a, b) -> {
					int div = a / b;
					int mod = a % b;
					return 0 <= mod ? Pair.of(div, mod) : Pair.of(div - 1, mod + b);
				}, //
				node -> node instanceof Int ? Opt.of(((Int) node).number) : Opt.none(), //
				ex::intOf);
	}

	public Fractional( //
			Ring<I> ring0, //
			Predicate<I> isPositive, //
			Fun2<I, I, Pair<I, I>> divMod_, //
			Fun<Node, Opt<I>> parse, //
			Fun<I, Node> format) {
		this.n0 = ring0.n0;
		this.n1 = ring0.n1;
		this.isPositive = isPositive;
		this.isZero = a -> !isPositive.test(a) && !isPositive.test(neg_.apply(a));
		this.add_ = ring0.add;
		this.neg_ = ring0.neg;
		this.mul_ = ring0.mul;
		this.divMod_ = divMod_;
		this.parse = parse;
		this.format = format;

		f0 = new Fract(n0, n1);
		f1 = new Fract(n1, n1);
		ring = new Ring<>(f0, f1, this::add, this::neg, this::mul);
	}

	public Opt<Pair<I, I>> fractionalize(Node node) {
		return parse(node).map(fraction -> Pair.of(fraction.t0, fraction.t1));
	}

	public Opt<Fract> parse(Node node) {
		Fractional<I> fr = Fractional.this;

		return new Object() {
			private Opt<Fract> fract(Node node) {
				return new SwitchNode<Opt<Fract>>(node //
				).match2(patAdd, (a, b) -> {
					return fract(a).join(fract(b), fr::add);
				}).match1(patNeg, a -> {
					return fract(a).map(fr::neg);
				}).match2(patMul, (a, b) -> {
					return fract(a).join(fract(b), fr::mul);
				}).match1(patInv, a -> {
					return inv1(fract(a));
				}).match2(patPow, (a, b) -> {
					return b instanceof Int ? pow(a, ((Int) b).number) : Opt.none();
				}).applyIf(Node.class, a -> {
					return parse.apply(a).map(i -> new Fract(i, n1));
				}).nonNullResult();
			}

			private Opt<Fract> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return fract(a).map(pair -> { // TODO assummed a != 0 or b != 0
						Fract r = f1;
						for (char ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Fract> inv1(Opt<Fract> opt) {
				return opt.concatMap(fr::inv);
			}
		}.fract(node).map(fraction -> {
			Gcd gcd = new Gcd(fraction.t0, fraction.t1, 9);
			return new Fract(gcd.m0, gcd.m1);
		});
	}

	public Node format(Fract fract) {
		OpGroup add = ex.add;
		OpGroup mul = ex.mul;

		Fun2<I, I, Node> f = (n, d) -> {
			Node i0 = format.apply(n);
			Node i1 = format.apply(d);
			return mul.apply(i0, mul.inverse(i1));
		};

		I n_ = fract.t0;
		I d_ = fract.t1;
		I nn = neg_.apply(n_);

		if (!isPositive.test(nn))
			return f.apply(n_, d_);
		else
			return add.inverse(f.apply(nn, d_));
	}

	public Fract f0;
	public Fract f1;
	public Ring<Fract> ring;

	private Opt<Fract> inv(Fract a) {
		I num = a.t0;
		I denom = a.t1;
		I numn = neg_.apply(num);
		if (isPositive.test(num))
			return Opt.of(new Fract(denom, num));
		else if (isPositive.test(numn))
			return Opt.of(new Fract(neg_.apply(denom), numn));
		else
			return Opt.none();
	}

	public Fract inverse(Fract a) {
		I num = a.t0;
		I denom = a.t1;
		if (isPositive.test(num))
			return new Fract(denom, num);
		else
			return new Fract(neg_.apply(denom), neg_.apply(num));
	}

	private Fract mul(Fract a, Fract b) {
		Gcd gcd = new Gcd(mul_.apply(a.t0, b.t0), mul_.apply(a.t1, b.t1), 9);
		return new Fract(gcd.m0, gcd.m1);
	}

	private Fract neg(Fract a) {
		return new Fract(neg_.apply(a.t0), a.t1);
	}

	private Fract add(Fract a, Fract b) {
		Gcd gcd = new Gcd(a.t1, b.t1, 9);
		I num0 = mul_.apply(a.t0, gcd.m1);
		I num1 = mul_.apply(b.t0, gcd.m0);
		I denom = mul_.apply(gcd.gcd, mul_.apply(gcd.m0, gcd.m1));
		return new Fract(add_.apply(num0, num1), denom);
	}

	private class Gcd {
		private I gcd;
		private I m0, m1;

		private Gcd(I n, I d, int depth) {
			if (isZero.test(d)) {
				gcd = n;
				m0 = n1;
				m1 = d;
			} else if (depth <= 0) {
				gcd = n1;
				m0 = n;
				m1 = d;
			} else {
				Pair<I, I> divMod = divMod_.apply(n, d);
				I f = divMod.t0; // div_.apply(n, d);
				I ndf = divMod.t1; // add_.apply(n, neg_.apply(df));
				Gcd gcd1 = new Gcd(d, ndf, depth - 1);

				// n = gcd1.gcd * (gcd1.m0 * f + gcd1.m1)
				// d = gcd1.gcd * gcd1.m0
				gcd = gcd1.gcd;
				m0 = add_.apply(mul_.apply(gcd1.m0, f), gcd1.m1);
				m1 = gcd1.m0;
			}
		}
	}

	public class Fract {
		private I t0, t1;

		private Fract(I t0, I t1) {
			this.t0 = t0;
			this.t1 = t1;
		}
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;

}
