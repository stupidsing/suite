package suite.adt;

import java.util.Objects;
import java.util.function.Predicate;

import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
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

	public <U> Opt<U> concatMap(Fun<T, Opt<U>> fun) {
		return !isEmpty() ? fun.apply(value) : Opt.none();
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

	public <U, V> Opt<V> join(Opt<U> opt1, Fun2<T, U, V> fun) {
		return !isEmpty() && !opt1.isEmpty() ? Opt.of(fun.apply(value, opt1.value)) : Opt.none();
	}

	public T get() {
		return !isEmpty() ? value : Fail.t("no result");
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	public <U> Opt<U> map(Fun<T, U> fun) {
		return !isEmpty() ? of(fun.apply(value)) : none();
	}

	public T or(Source<T> or) {
		return !isEmpty() ? value : or.source();
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "null";
	}

}
