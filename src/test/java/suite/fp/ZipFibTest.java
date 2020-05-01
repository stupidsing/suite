package suite.fp;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Supplier;

//fibs = 0 : 1 : zipWith (+) fibs (tail fibs)
public class ZipFibTest {

	public interface T {
	}

	public interface Thunk extends Supplier<T> {
	}

	public interface Fn<R> extends Function<Thunk, R> {
	}

	public class N_ implements T {
		private int i;

		private N_(int i) {
			this.i = i;
		}
	}

	public class Cons implements T {
		private Thunk head;
		private Thunk tail;

		private Cons(Thunk t0, Thunk t1) {
			this.head = t0;
			this.tail = t1;
		}
	}

	@Test
	public void test() {
		Thunk zero = () -> new N_(0);
		Thunk one = () -> new N_(1);
		Fn<Fn<Thunk>> cons = a -> b -> () -> new Cons(a, b);
		Fn<Thunk> fst = list_ -> () -> ((Cons) list_.get()).head.get();
		Fn<Thunk> snd = list_ -> () -> ((Cons) list_.get()).tail.get();
		Fn<Fn<Thunk>> add = a -> b -> () -> new N_(((N_) a.get()).i + ((N_) b.get()).i);

		var take = new Fn<Fn<Thunk>>() {
			public Fn<Thunk> apply(Thunk i_) {
				var i = ((N_) i_.get()).i;
				return i != 0 //
						? list_ -> apply(() -> new N_(i - 1)).apply(snd.apply(list_)) //
						: fst::apply;
			}
		};

		var zipAdd = new Fn<Fn<Thunk>>() {
			public Fn<Thunk> apply(Thunk l0) {
				return l1 -> () -> new Cons( //
						() -> add.apply(fst.apply(l0)).apply(fst.apply(l1)).get(), //
						() -> apply(snd.apply(l0)).apply(snd.apply(l1)).get());
			}
		};

		var fibs = new Thunk[1];
		Thunk fib = () -> fibs[0].get();
		fibs[0] = cons.apply(zero).apply(cons.apply(one).apply(zipAdd.apply(fib).apply(snd.apply(fib))));

		var fib12 = take.apply(() -> new N_(12)).apply(fib);

		System.out.println(((N_) fib12.get()).i);
	}

}
