package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.adt.pair.Fixie_.Fixie0;
import suite.adt.pair.Fixie_.Fixie1;
import suite.adt.pair.Fixie_.Fixie2;
import suite.adt.pair.Fixie_.Fixie3;
import suite.adt.pair.Fixie_.Fixie4;
import suite.adt.pair.Fixie_.Fixie5;
import suite.adt.pair.Fixie_.Fixie6;
import suite.adt.pair.Fixie_.Fixie7;
import suite.adt.pair.Fixie_.Fixie8;
import suite.adt.pair.Fixie_.Fixie9;
import suite.adt.pair.Fixie_.FixieA;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.adt.pair.Fixie_.FixieFun7;
import suite.adt.pair.Fixie_.FixieFun8;
import suite.adt.pair.Fixie_.FixieFun9;
import suite.adt.pair.Fixie_.FixieFunA;
import suite.object.Object_;

public class Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> implements FixieA<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {

	private static D_ D = new D_();

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

	public static Fixie0 //
			of() {
		return of(D);
	}

	public static <T0> Fixie1<T0> //
			of(T0 t0) {
		return of(t0, D);
	}

	public static <T0, T1> Fixie2<T0, T1> //
			of(T0 t0, T1 t1) {
		return of(t0, t1, D);
	}

	public static <T0, T1, T2> Fixie3<T0, T1, T2> //
			of(T0 t0, T1 t1, T2 t2) {
		return of(t0, t1, t2, D);
	}

	public static <T0, T1, T2, T3> Fixie4<T0, T1, T2, T3> //
			of(T0 t0, T1 t1, T2 t2, T3 t3) {
		return of(t0, t1, t2, t3, D);
	}

	public static <T0, T1, T2, T3, T4> Fixie5<T0, T1, T2, T3, T4> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4) {
		return of(t0, t1, t2, t3, t4, D);
	}

	public static <T0, T1, T2, T3, T4, T5> Fixie6<T0, T1, T2, T3, T4, T5> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
		return of(t0, t1, t2, t3, t4, t5, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6> Fixie7<T0, T1, T2, T3, T4, T5, T6> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
		return of(t0, t1, t2, t3, t4, t5, t6, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7> Fixie8<T0, T1, T2, T3, T4, T5, T6, T7> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Fixie9<T0, T1, T2, T3, T4, T5, T6, T7, T8> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, t8, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
		return new Fixie<>(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public static class D_ {
	}

	protected Fixie(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
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

	@Override
	public <R> R map(FixieFun0<R> fun) {
		return fun.apply();
	}

	@Override
	public <R> R map(FixieFun1<T0, R> fun) {
		return fun.apply(t0);
	}

	@Override
	public <R> R map(FixieFun2<T0, T1, R> fun) {
		return fun.apply(t0, t1);
	}

	@Override
	public <R> R map(FixieFun3<T0, T1, T2, R> fun) {
		return fun.apply(t0, t1, t2);
	}

	@Override
	public <R> R map(FixieFun4<T0, T1, T2, T3, R> fun) {
		return fun.apply(t0, t1, t2, t3);
	}

	@Override
	public <R> R map(FixieFun5<T0, T1, T2, T3, T4, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4);
	}

	@Override
	public <R> R map(FixieFun6<T0, T1, T2, T3, T4, T5, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4, t5);
	}

	@Override
	public <R> R map(FixieFun7<T0, T1, T2, T3, T4, T5, T6, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4, t5, t6);
	}

	@Override
	public <R> R map(FixieFun8<T0, T1, T2, T3, T4, T5, T6, T7, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4, t5, t6, t7);
	}

	@Override
	public <R> R map(FixieFun9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4, t5, t6, t7, t8);
	}

	@Override
	public <R> R map(FixieFunA<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> fun) {
		return fun.apply(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public static < //
			T0 extends Comparable<? super T0>, //
			T1 extends Comparable<? super T1>, //
			T2 extends Comparable<? super T2>, //
			T3 extends Comparable<? super T3>, //
			T4 extends Comparable<? super T4>, //
			T5 extends Comparable<? super T5>, //
			T6 extends Comparable<? super T6>, //
			T7 extends Comparable<? super T7>, //
			T8 extends Comparable<? super T8>, //
			T9 extends Comparable<? super T9>> //
	Comparator<Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>> comparator() {
		return (fixie0, fixie1) -> {
			var c = 0;
			c = c == 0 ? Object_.compare(fixie0.t0, fixie1.t0) : c;
			c = c == 0 ? Object_.compare(fixie0.t1, fixie1.t1) : c;
			c = c == 0 ? Object_.compare(fixie0.t2, fixie1.t2) : c;
			c = c == 0 ? Object_.compare(fixie0.t3, fixie1.t3) : c;
			c = c == 0 ? Object_.compare(fixie0.t4, fixie1.t4) : c;
			c = c == 0 ? Object_.compare(fixie0.t5, fixie1.t5) : c;
			c = c == 0 ? Object_.compare(fixie0.t6, fixie1.t6) : c;
			c = c == 0 ? Object_.compare(fixie0.t7, fixie1.t7) : c;
			c = c == 0 ? Object_.compare(fixie0.t8, fixie1.t8) : c;
			c = c == 0 ? Object_.compare(fixie0.t9, fixie1.t9) : c;
			return c;
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Fixie.class) {
			var other = (Fixie<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
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
		var h = 7;
		h = h * 31 + Objects.hashCode(t0);
		h = h * 31 + Objects.hashCode(t1);
		h = h * 31 + Objects.hashCode(t2);
		h = h * 31 + Objects.hashCode(t3);
		h = h * 31 + Objects.hashCode(t4);
		h = h * 31 + Objects.hashCode(t5);
		h = h * 31 + Objects.hashCode(t6);
		h = h * 31 + Objects.hashCode(t7);
		h = h * 31 + Objects.hashCode(t8);
		h = h * 31 + Objects.hashCode(t9);
		return h;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		if (t0 != D)
			sb.append(t0.toString());
		if (t1 != D)
			sb.append(":" + t1.toString());
		if (t2 != D)
			sb.append(":" + t2.toString());
		if (t3 != D)
			sb.append(":" + t3.toString());
		if (t4 != D)
			sb.append(":" + t4.toString());
		if (t5 != D)
			sb.append(":" + t5.toString());
		if (t6 != D)
			sb.append(":" + t6.toString());
		if (t7 != D)
			sb.append(":" + t7.toString());
		if (t8 != D)
			sb.append(":" + t8.toString());
		if (t9 != D)
			sb.append(":" + t9.toString());
		return sb.toString();
	}

	@Override
	public T0 get0() {
		return t0;
	}

	@Override
	public T1 get1() {
		return t1;
	}

	@Override
	public T2 get2() {
		return t2;
	}

	@Override
	public T3 get3() {
		return t3;
	}

	@Override
	public T4 get4() {
		return t4;
	}

	@Override
	public T5 get5() {
		return t5;
	}

	@Override
	public T6 get6() {
		return t6;
	}

	@Override
	public T7 get7() {
		return t7;
	}

	@Override
	public T8 get8() {
		return t8;
	}

	@Override
	public T9 get9() {
		return t9;
	}

}
