package suite.math.sym;

import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil2.Fun2;

public class Sym {

	public static Sym me = new Sym();

	private Sym() {
	}

	public static class Field<T> extends Ring<T> {
		public final Iterate<T> inv;

		public Field(T n0, T n1, Fun2<T, T, T> add, Iterate<T> neg, Fun2<T, T, T> mul, Iterate<T> inv) {
			super(n0, n1, add, neg, mul);
			this.inv = inv;
		}
	}

	public static class Ring<T> extends Group<T> {
		public final T n1;
		public final Fun2<T, T, T> mul;

		public Ring(T n0, T n1, Fun2<T, T, T> add, Iterate<T> neg, Fun2<T, T, T> mul) {
			super(n0, add, neg);
			this.n1 = n1;
			this.mul = mul;
		}
	}

	public static class Group<T> extends Monoid<T> {
		public final Iterate<T> neg;

		public Group(T n0, Fun2<T, T, T> add, Iterate<T> neg) {
			super(n0, add);
			this.neg = neg;
		}
	}

	public static class Monoid<T> {
		public final T n0;
		public final Fun2<T, T, T> add;

		public Monoid(T n0, Fun2<T, T, T> add) {
			this.n0 = n0;
			this.add = add;
		}
	}

}
