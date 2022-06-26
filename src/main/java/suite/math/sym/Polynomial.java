package suite.math.sym;

import java.util.function.Predicate;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.adt.Opt;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.fp.Funs2.Fun2;
import primal.primitive.IntMoreVerbs.ReadInt;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.adt.map.IntObjMap;
import primal.streamlet.primitive.IntObjStreamlet;
import suite.BindArrayUtil.Pattern;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;

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
	private Fun<Node, Opt<N>> parse_;
	private Fun<N, Node> format_;

	public Polynomial( //
			Node x, //
			Predicate<Node> is_x, //
			Ring<N> ring0, //
			Obj_Int<N> sgn_, //
			Fun<Node, Opt<N>> parse_, //
			Fun<N, Node> format_) {
		this.n0 = ring0.n0;
		this.n1 = ring0.n1;
		this.x = x;
		this.is_x = is_x;
		this.sgn_ = sgn_;
		this.add_ = ring0.add;
		this.neg_ = ring0.neg;
		this.mul_ = ring0.mul;
		this.parse_ = parse_;
		this.format_ = format_;

		p0 = new Poly<>(this);
		p1 = polyOf(0, n1);
		px = polyOf(1, n1);
		ring = new Ring<>(p0, p1, this::add, this::neg, this::mul);
	}

	public Opt<Poly<N>> parse(Node node) { // polynomialize
		var py = Polynomial.this;

		return new Object() {
			private Opt<Poly<N>> poly(Node node) {
				return new SwitchNode<Opt<Poly<N>>>(node //
				).match(patAdd, (a, b) -> {
					return poly(a).join(poly(b), py::add);
				}).match(patNeg, a -> {
					return poly(a).map(py::neg);
				}).match(patMul, (a, b) -> {
					return poly(a).join(poly(b), py::mul);
				}).match(patInv, a -> {
					return Opt.none();
				}).match(patPow, (a, b) -> {
					return b instanceof Int i ? pow(a, i.number) : Opt.none();
				}).applyIf(Node.class, a -> {
					if (is_x.test(a))
						return Opt.of(px);
					else
						return parse_.apply(a).map(n -> polyOf(0, n));
				}).nonNullResult();
			}

			private Opt<Poly<N>> pow(Node a, int power) {
				if (power < 0)
					return Opt.none();
				else
					return poly(a).map(pair -> { // TODO assummed a != 0 or b != 0
						var r = p1;
						for (var ch : Integer.toBinaryString(power).toCharArray()) {
							r = mul(r, r);
							r = ch != '0' ? mul(r, pair) : r;
						}
						return r;
					});
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

	private Poly<N> mul(Poly<N> a, Poly<N> b) {
		var c = polyOf();
		for (var pair0 : ReadInt.from2(a))
			for (var pair1 : ReadInt.from2(b))
				c.add(pair0.k + pair1.k, mul_.apply(pair0.v, pair1.v));
		return c;
	}

	private Poly<N> neg(Poly<N> a) {
		return polyOf(ReadInt.from2(a).mapValue(neg_));
	}

	private Poly<N> add(Poly<N> a, Poly<N> b) {
		var c = polyOf();
		for (var pair : IntObjStreamlet.concat(ReadInt.from2(a), ReadInt.from2(b)))
			c.add(pair.k, pair.v);
		return c;
	}

	public int sign(Poly<N> a) {
		return 0 < a.size() ? sgn_.apply(a.decons().get1()) : 0;
	}

	public Poly<N> polyOf(IntObjStreamlet<N> map) {
		return new Poly<>(this, map);
	}

	public Poly<N> polyOf(int power, N term) {
		var poly = polyOf();
		poly.add(power, term);
		return poly;
	}

	public Poly<N> polyOf() {
		return new Poly<>(this);
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
			var max = streamlet().keys().min((p0, p1) -> p1 - p0);
			return Fixie.of(max, get(max), new Poly<>(py, streamlet().filterKey(p -> p != max)));
		}

		public String toString() {
			return streamlet() //
					.sortByKey((p0, p1) -> p1 - p0) //
					.map((p, t) -> t + " * " + py.x + "^" + p) //
					.toJoinedString(" + ");
		}

		private void add(int power, N term) {
			var n0 = py.n0;
			Iterate<N> i0 = t -> t != null ? t : n0;
			Iterate<N> ix = t -> t != n0 ? t : null;
			update(power, t -> ix.apply(py.add_.apply(i0.apply(t), term)));
		}

		private IntObjStreamlet<N> streamlet() {
			return ReadInt.from2(this);
		}
	}

	private Pattern patAdd = ex.patAdd;
	private Pattern patNeg = ex.patNeg;
	private Pattern patMul = ex.patMul;
	private Pattern patInv = ex.patInv;
	private Pattern patPow = ex.patPow;

}
