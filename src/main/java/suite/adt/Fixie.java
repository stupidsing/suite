package suite.adt;

import java.util.Comparator;
import java.util.Objects;

import suite.util.Util;

public class Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {

	private static final D_ D = new D_();

	public T0 t0;
	public T1 t1;
	public T2 t2;
	public T3 t3;
	public T4 t4;
	public T5 t5;
	public T6 t6;
	public T7 t7;
	public T8 t8;
	public T9 t9;

	public static Fixie<D_, D_, D_, D_, D_, D_, D_, D_, D_, D_> //
			of() {
		return of(D);
	}

	public static <T0> Fixie<T0, D_, D_, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0) {
		return of(t0, D);
	}

	public static <T0, T1> Fixie<T0, T1, D_, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1) {
		return of(t0, t1, D);
	}

	public static <T0, T1, T2> Fixie<T0, T1, T2, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2) {
		return of(t0, t1, t2, D);
	}

	public static <T0, T1, T2, T3> Fixie<T0, T1, T2, T3, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3) {
		return of(t0, t1, t2, t3, D);
	}

	public static <T0, T1, T2, T3, T4> Fixie<T0, T1, T2, T3, T4, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4) {
		return of(t0, t1, t2, t3, t4, D);
	}

	public static <T0, T1, T2, T3, T4, T5> Fixie<T0, T1, T2, T3, T4, T5, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
		return of(t0, t1, t2, t3, t4, t5, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6> Fixie<T0, T1, T2, T3, T4, T5, T6, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
		return of(t0, t1, t2, t3, t4, t5, t6, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, t8, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
		return new Fixie<>(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public static class D_ {
	}

	private Fixie(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
		this.t0 = t0;
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.t4 = t4;
		this.t5 = t5;
		this.t6 = t6;
		this.t7 = t7;
		this.t8 = t8;
		this.t9 = t9;
	}

	public static //
	<T0 extends Comparable<? super T0> //
			, T1 extends Comparable<? super T1> //
			, T2 extends Comparable<? super T2> //
			, T3 extends Comparable<? super T3> //
			, T4 extends Comparable<? super T4> //
			, T5 extends Comparable<? super T5> //
			, T6 extends Comparable<? super T6> //
			, T7 extends Comparable<? super T7> //
			, T8 extends Comparable<? super T8> //
			, T9 extends Comparable<? super T9> //
	> Comparator<Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>> comparator() {
		return (fixie0, fixie1) -> {
			int c = 0;
			c = c == 0 ? Util.compare(fixie0.t0, fixie1.t0) : c;
			c = c == 0 ? Util.compare(fixie0.t1, fixie1.t1) : c;
			c = c == 0 ? Util.compare(fixie0.t2, fixie1.t2) : c;
			c = c == 0 ? Util.compare(fixie0.t3, fixie1.t3) : c;
			c = c == 0 ? Util.compare(fixie0.t4, fixie1.t4) : c;
			c = c == 0 ? Util.compare(fixie0.t5, fixie1.t5) : c;
			c = c == 0 ? Util.compare(fixie0.t6, fixie1.t6) : c;
			c = c == 0 ? Util.compare(fixie0.t7, fixie1.t7) : c;
			c = c == 0 ? Util.compare(fixie0.t8, fixie1.t8) : c;
			c = c == 0 ? Util.compare(fixie0.t9, fixie1.t9) : c;
			return c;
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Fixie.class) {
			Fixie<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Fixie<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
			return true //
					&& Objects.equals(t0, other.t0) //
					&& Objects.equals(t1, other.t1) //
					&& Objects.equals(t2, other.t2) //
					&& Objects.equals(t3, other.t3) //
					&& Objects.equals(t4, other.t4) //
					&& Objects.equals(t5, other.t5) //
					&& Objects.equals(t6, other.t6) //
					&& Objects.equals(t7, other.t7) //
					&& Objects.equals(t8, other.t8) //
					&& Objects.equals(t9, other.t9) //
			;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return 0 //
				^ Objects.hashCode(t0) //
				^ Objects.hashCode(t1) //
				^ Objects.hashCode(t2) //
				^ Objects.hashCode(t3) //
				^ Objects.hashCode(t4) //
				^ Objects.hashCode(t5) //
				^ Objects.hashCode(t6) //
				^ Objects.hashCode(t7) //
				^ Objects.hashCode(t8) //
				^ Objects.hashCode(t9);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean cont = true;
		if (cont &= t0 != D)
			sb.append(t0.toString());
		if (cont &= t1 != D)
			sb.append(":" + t1.toString());
		if (cont &= t2 != D)
			sb.append(":" + t2.toString());
		if (cont &= t3 != D)
			sb.append(":" + t3.toString());
		if (cont &= t4 != D)
			sb.append(":" + t4.toString());
		if (cont &= t5 != D)
			sb.append(":" + t5.toString());
		if (cont &= t6 != D)
			sb.append(":" + t6.toString());
		if (cont &= t7 != D)
			sb.append(":" + t7.toString());
		if (cont &= t8 != D)
			sb.append(":" + t8.toString());
		if (cont &= t9 != D)
			sb.append(":" + t9.toString());
		return sb.toString();
	}

}
