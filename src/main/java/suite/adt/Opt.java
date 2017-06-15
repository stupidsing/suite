package suite.adt;

import java.util.function.Predicate;

import suite.util.FunUtil.Fun;

public class Opt<T> {

	private static Opt<?> none_ = Opt.of(null);
	private T value;

	@SuppressWarnings("unchecked")
	public static <T> Opt<T> none() {
		return (Opt<T>) none_;
	}

	public static <T> Opt<T> of(T t) {
		Opt<T> p = new Opt<>();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == null;
	}

	public Opt<T> filter(Predicate<T> pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <U> Opt<U> map(Fun<T, U> fun) {
		return !isEmpty() ? of(fun.apply(value)) : none();
	}

	public T get() {
		return value;
	}

}
