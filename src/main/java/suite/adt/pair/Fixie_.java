package suite.adt.pair;

public class Fixie_ {

	public interface Get0<T0> {
		public T0 get0();
	}

	public interface Get1<T0, T1> extends Get0<T0> {
		public T1 get1();
	}

	public interface Get2<T0, T1, T2> extends Get1<T0, T1> {
		public T2 get2();
	}

	public interface Get3<T0, T1, T2, T3> extends Get2<T0, T1, T2> {
		public T3 get3();
	}

	public interface Get4<T0, T1, T2, T3, T4> extends Get3<T0, T1, T2, T3> {
		public T4 get4();
	}

	public interface Get5<T0, T1, T2, T3, T4, T5> extends Get4<T0, T1, T2, T3, T4> {
		public T5 get5();
	}

	public interface Get6<T0, T1, T2, T3, T4, T5, T6> extends Get5<T0, T1, T2, T3, T4, T5> {
		public T6 get6();
	}

	public interface Get7<T0, T1, T2, T3, T4, T5, T6, T7> extends Get6<T0, T1, T2, T3, T4, T5, T6> {
		public T7 get7();
	}

	public interface Get8<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends Get7<T0, T1, T2, T3, T4, T5, T6, T7> {
		public T8 get8();
	}

	public interface Get9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Get8<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
		public T9 get9();
	}

}
