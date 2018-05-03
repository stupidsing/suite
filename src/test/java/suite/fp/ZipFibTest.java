package suite.fp;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

//fibs = 0 : 1 : zipWith (+) fibs (tail fibs)
public class ZipFibTest {

	public interface T {
	}

	public interface Thunk extends Supplier<T> {
	}

	public interface Fun<R> extends Function<Thunk, R> {
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
		Fun<Fun<Thunk>> cons = a -> b -> () -> new Cons(a, b);
		Fun<Thunk> fst = list_ -> () -> ((Cons) list_.get()).head.get();
		Fun<Thunk> snd = list_ -> () -> ((Cons) list_.get()).tail.get();
		Fun<Fun<Thunk>> add = a -> b -> () -> new N_(((N_) a.get()).i + ((N_) b.get()).i);

		var take_ = new Object() {
			Fun<Thunk> take(Thunk i_) {
				var i = ((N_) i_.get()).i;
				return i != 0 //
						? list_ -> take(() -> new N_(i - 1)).apply(snd.apply(list_)) //
						: fst::apply;
			}
		};

		var zipAdd_ = new Object() {
			Fun<Thunk> zipAdd(Thunk l0) {
				return l1 -> () -> new Cons( //
						() -> add.apply(fst.apply(l0)).apply(fst.apply(l1)).get(), //
						() -> zipAdd(snd.apply(l0)).apply(snd.apply(l1)).get());
			}
		};

		Fun<Fun<Thunk>> take = take_::take;
		Fun<Fun<Thunk>> zipAdd = zipAdd_::zipAdd;

		var fibs = new Thunk[1];
		Thunk fib = () -> fibs[0].get();
		fibs[0] = cons.apply(zero).apply(cons.apply(one).apply(zipAdd.apply(fib).apply(snd.apply(fib))));

		var fib12 = take.apply(() -> new N_(12)).apply(fib);

		System.out.println(((N_) fib12.get()).i);
	}

}
