package suite.math.sym;

import suite.BindArrayUtil.Pattern;
import suite.adt.Opt;
import suite.adt.pair.Pair;
import suite.math.sym.Sym.Field;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil2.Fun2;

public class Fractional<I> {

	private static Express ex = new Express();

	private I n0;
	private I n1;
	private Obj_Int<I> sgn_;
	private Fun2<I, I, I> add_;
	private Iterate<I> neg_;
	private Fun2<I, I, I> mul_;
	private Fun2<I, I, Pair<I, I>> divMod_;
	private Fun<Node, Opt<I>> parse_;
	private Fun<I, Node> format_;

	public static Fractional<Integer> ofIntegral() {
		var integral = new Integral();
		return new Fractional<>( //
				integral.ring, //
				integral::sign, //
				integral::divMod, //
				integral::parse, //
				integral::format);
	}

	public Fractional( //
			Ring<I> ring0, //
			Obj_Int<I> sgn_, //
			Fun2<I, I, Pair<I, I>> divMod_, //
			Fun<Node, Opt<I>> parse_, //
			Fun<I, Node> format_) {
		this.n0 = ring0.n0;
		this.n1 = ring0.n1;
		this.sgn_ = sgn_;
		this.add_ = ring0.add;
		this.neg_ = ring0.neg;
		this.mul_ = ring0.mul;
		this.divMod_ = divMod_;
		this.parse_ = parse_;
		this.format_ = format_;

		f0 = new Fract<>(n0, n1);
		f1 = new Fract<>(n1, n1);
		field = new Field<>(f0, f1, this::add, this::neg, this::mul, this::inverse);
	}

	public Opt<Pair<I, I>> fractionalize(Node node) {
		return parse(node).map(fraction -> Pair.of(fraction.t0, fraction.t1));
	}

	public Opt<Fract<I>> parse(Node node) {
		Fractional<I> fr = Fractional.this;

		return new Object() {
			private Opt<Fract<I>> fract(Node node) {
				return new SwitchNode<Opt<Fract<I>>>(node //
				).match(patAdd, (a, b) -> {
					return fract(a).join(fract(b), fr::add);
				}).match(patNeg, a -> {
					return fract(a).map(fr::neg);
				}).match(patMul, (a, b) -> {
					return fract(a).join(fract(b), fr::mul);
				}).match(patInv, a -> {
					return inv1(fract(a));
				}).match(patPow, (a, b) -> {
					return b instanceof Int ? pow(a, Int.num(b)) : Opt.none();
				}).applyIf(Node.class, a -> {
					return parse_.apply(a).map(i -> new Fract<>(i, n1));
				}).nonNullResult();
			}

			private Opt<Fract<I>> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return fract(a).map(pair -> { // TODO assummed a != 0 or b != 0
						Fract<I> r = f1;
						for (var ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Fract<I>> inv1(Opt<Fract<I>> opt) {
				return opt.concatMap(fr::inv);
			}
		}.fract(node).map(fraction -> {
			var gcd = new Gcd(fraction.t0, fraction.t1, 9);
			return new Fract<>(gcd.m0, gcd.m1);
		});
	}

	public Node format(Fract<I> fract) {
		var add = ex.add;
		var mul = ex.mul;

		Fun2<I, I, Node> f = (n, d) -> {
			var i0 = format_.apply(n);
			var i1 = format_.apply(d);
			return mul.apply(i0, mul.inverse(i1));
		};

		var n_ = fract.t0;
		var d_ = fract.t1;
		var nn = neg_.apply(n_);

		if (0 <= sgn_.apply(n_))
			return f.apply(n_, d_);
		else
			return add.inverse(f.apply(nn, d_));
	}

	public Fract<I> f0;
	public Fract<I> f1;
	public Field<Fract<I>> field;

	private Opt<Fract<I>> inv(Fract<I> a) {
		var num = a.t0;
		var denom = a.t1;
		var c = sgn_.apply(num);
		if (0 < c)
			return Opt.of(new Fract<>(denom, num));
		else if (c < 0)
			return Opt.of(new Fract<>(neg_.apply(denom), neg_.apply(num)));
		else
			return Opt.none();
	}

	public Fract<I> inverse(Fract<I> a) {
		var num = a.t0;
		var denom = a.t1;
		if (0 <= sgn_.apply(num))
			return new Fract<>(denom, num);
		else
			return new Fract<>(neg_.apply(denom), neg_.apply(num));
	}

	private Fract<I> mul(Fract<I> a, Fract<I> b) {
		var gcd = new Gcd(mul_.apply(a.t0, b.t0), mul_.apply(a.t1, b.t1), 9);
		return new Fract<>(gcd.m0, gcd.m1);
	}

	private Fract<I> neg(Fract<I> a) {
		return new Fract<>(neg_.apply(a.t0), a.t1);
	}

	private Fract<I> add(Fract<I> a, Fract<I> b) {
		var gcd = new Gcd(a.t1, b.t1, 9);
		var num0 = mul_.apply(a.t0, gcd.m1);
		var num1 = mul_.apply(b.t0, gcd.m0);
		var denom = mul_.apply(gcd.gcd, mul_.apply(gcd.m0, gcd.m1));
		return new Fract<>(add_.apply(num0, num1), denom);
	}

	public int sign(Fract<I> a) {
		return sgn_.apply(a.t0);
	}

	private class Gcd {
		private I gcd;
		private I m0, m1;

		private Gcd(I n, I d, int depth) {
			if (sgn_.apply(d) == 0) {
				gcd = n;
				m0 = n1;
				m1 = d;
			} else if (depth <= 0) {
				gcd = n1;
				m0 = n;
				m1 = d;
			} else {
				var divMod = divMod_.apply(n, d);
				var f = divMod.k; // div_.apply(n, d);
				var ndf = divMod.v; // add_.apply(n, neg_.apply(df));
				var gcd1 = new Gcd(d, ndf, depth - 1);

				// n = gcd1.gcd * (gcd1.m0 * f + gcd1.m1)
				// d = gcd1.gcd * gcd1.m0
				gcd = gcd1.gcd;
				m0 = add_.apply(mul_.apply(gcd1.m0, f), gcd1.m1);
				m1 = gcd1.m0;
			}
		}
	}

	public static class Fract<I> {
		private I t0, t1;

		private Fract(I t0, I t1) {
			this.t0 = t0;
			this.t1 = t1;
		}

		public String toString() {
			return "F[" + t0 + ", " + t1 + "]";
		}
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;

}
