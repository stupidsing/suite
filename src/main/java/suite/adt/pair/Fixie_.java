package suite.adt.pair;

public class Fixie_ {

	public interface Fixie1<T0> {
		public T0 get0();
	}

	public interface Fixie2<T0, T1> extends Fixie1<T0> {
		public T1 get1();
	}

	public interface Fixie3<T0, T1, T2> extends Fixie2<T0, T1> {
		public T2 get2();
	}

	public interface Fixie4<T0, T1, T2, T3> extends Fixie3<T0, T1, T2> {
		public T3 get3();
	}

	public interface Fixie5<T0, T1, T2, T3, T4> extends Fixie4<T0, T1, T2, T3> {
		public T4 get4();
	}

	public interface Fixie6<T0, T1, T2, T3, T4, T5> extends Fixie5<T0, T1, T2, T3, T4> {
		public T5 get5();
	}

	public interface Fixie7<T0, T1, T2, T3, T4, T5, T6> extends Fixie6<T0, T1, T2, T3, T4, T5> {
		public T6 get6();
	}

	public interface Fixie8<T0, T1, T2, T3, T4, T5, T6, T7> extends Fixie7<T0, T1, T2, T3, T4, T5, T6> {
		public T7 get7();
	}

	public interface Fixie9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends Fixie8<T0, T1, T2, T3, T4, T5, T6, T7> {
		public T8 get8();
	}

}
