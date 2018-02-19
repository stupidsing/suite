package suite.math;

import java.util.function.Predicate;

import suite.BindArrayUtil.Pattern;
import suite.adt.Opt;
import suite.adt.pair.Pair;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Fun2;

public class Fraction_<I> {

	private I N1;
	private Predicate<I> isPositive;
	private Predicate<I> isZero;
	private Fun2<I, I, I> add_;
	private Iterate<I> neg_;
	private Fun2<I, I, I> mul_;
	private Fun2<I, I, Pair<I, I>> divMod_;
	private Fun<Node, Opt<I>> parse;

	public static Fraction_<Integer> ofRational() {
		return new Fraction_<>( //
				1, //
				a -> 0 < a, //
				(a, b) -> a + b, //
				a -> -a, //
				(a, b) -> a * b, //
				(a, b) -> {
					int div = a / b;
					int mod = a % b;
					return 0 <= mod ? Pair.of(div, mod) : Pair.of(div - 1, mod + b);
				}, //
				node -> node instanceof Int ? Opt.of(((Int) node).number) : Opt.none());
	}

	public Fraction_( //
			I n1, //
			Predicate<I> isPositive, //
			Fun2<I, I, I> add_, //
			Iterate<I> neg_, //
			Fun2<I, I, I> mul_, //
			Fun2<I, I, Pair<I, I>> divMod_, //
			Fun<Node, Opt<I>> parse) {
		N1 = n1;
		this.isPositive = isPositive;
		this.isZero = a -> !isPositive.test(a) && !isPositive.test(neg_.apply(a));
		this.add_ = add_;
		this.neg_ = neg_;
		this.mul_ = mul_;
		this.divMod_ = divMod_;
		this.parse = parse;
	}

	public Opt<Pair<I, I>> rational(Node node) {
		class Fraction {
			I t0, t1;

			Fraction(I t0, I t1) {
				this.t0 = t0;
				this.t1 = t1;
			}
		}

		class Gcd {
			I gcd;
			I m0, m1;

			Gcd(I n, I d, int depth) {
				if (isZero.test(d)) {
					gcd = n;
					m0 = N1;
					m1 = d;
				} else if (depth <= 0) {
					gcd = N1;
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

		return new Object() {
			private Opt<Fraction> rat(Node node) {
				return new SwitchNode<Opt<Fraction>>(node //
				).match2(patAdd, (a, b) -> {
					return rat(a).join(rat(b), this::add);
				}).match1(patNeg, a -> {
					return rat(a).map(this::neg);
				}).match2(patMul, (a, b) -> {
					return rat(a).join(rat(b), this::mul);
				}).match1(patInv, a -> {
					return inv1(rat(a));
				}).match2(patPow, (a, b) -> {
					return b instanceof Int ? pow(a, ((Int) b).number) : Opt.none();
				}).applyIf(Node.class, a -> {
					return parse.apply(a).map(i -> new Fraction(i, N1));
				}).nonNullResult();
			}

			private Opt<Fraction> pow(Node a, int power) {
				if (power < 0)
					return inv1(pow(a, -power));
				else
					return rat(a).map(pair -> { // TODO assummed a != 0 or b != 0
						Fraction r = new Fraction(N1, N1);
						for (char ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
			}

			private Opt<Fraction> inv1(Opt<Fraction> opt) {
				return opt.concatMap(this::inv);
			}

			private Opt<Fraction> inv(Fraction a) {
				I num = a.t0;
				I denom = a.t1;
				I numn = neg_.apply(num);
				if (isPositive.test(num))
					return Opt.of(new Fraction(denom, num));
				else if (isPositive.test(numn))
					return Opt.of(new Fraction(neg_.apply(denom), numn));
				else
					return Opt.none();
			}

			private Fraction mul(Fraction a, Fraction b) {
				Gcd gcd = new Gcd(mul_.apply(a.t0, b.t0), mul_.apply(a.t1, b.t1), 9);
				return new Fraction(gcd.m0, gcd.m1);
			}

			private Fraction neg(Fraction a) {
				return new Fraction(neg_.apply(a.t0), a.t1);
			}

			private Fraction add(Fraction a, Fraction b) {
				Gcd gcd = new Gcd(a.t1, b.t1, 9);
				I num0 = mul_.apply(a.t0, gcd.m1);
				I num1 = mul_.apply(b.t0, gcd.m0);
				I denom = mul_.apply(gcd.gcd, mul_.apply(gcd.m0, gcd.m1));
				return new Fraction(add_.apply(num0, num1), denom);
			}
		}.rat(node).map(fraction -> Pair.of(fraction.t0, fraction.t1));
	}

	private Pattern patAdd = Sym.me.patAdd;
	private Pattern patNeg = Sym.me.patNeg;
	private Pattern patMul = Sym.me.patMul;
	private Pattern patInv = Sym.me.patInv;
	private Pattern patPow = Sym.me.patPow;

}
