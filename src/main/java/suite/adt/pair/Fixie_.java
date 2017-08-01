package suite.adt.pair;

public class Fixie_ {

	public interface FixieFun0 {
	}

	public interface FixieFun1<T0, R> {
		public R apply(T0 t0);
	}

	public interface FixieFun2<T0, T1, R> {
		public R apply(T0 t0, T1 t1);
	}

	public interface FixieFun3<T0, T1, T2, R> {
		public R apply(T0 t0, T1 t1, T2 t2);
	}

	public interface FixieFun4<T0, T1, T2, T3, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3);
	}

	public interface FixieFun5<T0, T1, T2, T3, T4, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4);
	}

	public interface FixieFun6<T0, T1, T2, T3, T4, T5, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
	}

	public interface FixieFun7<T0, T1, T2, T3, T4, T5, T6, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
	}

	public interface FixieFun8<T0, T1, T2, T3, T4, T5, T6, T7, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);
	}

	public interface FixieFun9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);
	}

	public interface FixieFunA<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {
		public R apply(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9);
	}

	public interface Fixie0 {
	}

	public interface Fixie1<T0> extends Fixie0 {
		public T0 get0();

		public <R> R map(FixieFun1<T0, R> fun);
	}

	public interface Fixie2<T0, T1> extends Fixie1<T0> {
		public T1 get1();

		public <R> R map(FixieFun2<T0, T1, R> fun);
	}

	public interface Fixie3<T0, T1, T2> extends Fixie2<T0, T1> {
		public T2 get2();

		public <R> R map(FixieFun3<T0, T1, T2, R> fun);
	}

	public interface Fixie4<T0, T1, T2, T3> extends Fixie3<T0, T1, T2> {
		public T3 get3();

		public <R> R map(FixieFun4<T0, T1, T2, T3, R> fun);
	}

	public interface Fixie5<T0, T1, T2, T3, T4> extends Fixie4<T0, T1, T2, T3> {
		public T4 get4();

		public <R> R map(FixieFun5<T0, T1, T2, T3, T4, R> fun);
	}

	public interface Fixie6<T0, T1, T2, T3, T4, T5> extends Fixie5<T0, T1, T2, T3, T4> {
		public T5 get5();

		public <R> R map(FixieFun6<T0, T1, T2, T3, T4, T5, R> fun);
	}

	public interface Fixie7<T0, T1, T2, T3, T4, T5, T6> extends Fixie6<T0, T1, T2, T3, T4, T5> {
		public T6 get6();

		public <R> R map(FixieFun7<T0, T1, T2, T3, T4, T5, T6, R> fun);
	}

	public interface Fixie8<T0, T1, T2, T3, T4, T5, T6, T7> extends Fixie7<T0, T1, T2, T3, T4, T5, T6> {
		public T7 get7();

		public <R> R map(FixieFun8<T0, T1, T2, T3, T4, T5, T6, T7, R> fun);
	}

	public interface Fixie9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends Fixie8<T0, T1, T2, T3, T4, T5, T6, T7> {
		public T8 get8();

		public <R> R map(FixieFun9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> fun);
	}

	public interface FixieA<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Fixie9<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
		public T9 get9();

		public <R> R map(FixieFunA<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> fun);
	}

}
