package suite.adt;

import java.util.Objects;
import java.util.function.Predicate;

import suite.util.FunUtil.Fun;
import suite.util.Object_;

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

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Opt.class && Objects.equals(value, ((Opt<?>) object).value);
	}

	public Opt<T> filter(Predicate<T> pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public boolean isEmpty() {
		return value == null;
	}

	public T get() {
		if (!isEmpty())
			return value;
		else
			throw new RuntimeException("no result");
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	public <U> Opt<U> map(Fun<T, U> fun) {
		return !isEmpty() ? of(fun.apply(value)) : none();
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "null";
	}

}
