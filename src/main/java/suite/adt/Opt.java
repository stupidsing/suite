package suite.adt; import static suite.util.Friends.fail;

import java.util.Objects;
import java.util.function.Predicate;

import suite.object.Object_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;

public class Opt<T> {

	private static Opt<?> none_ = Opt.of(null);
	private T value;

	@SuppressWarnings("unchecked")
	public static <T> Opt<T> none() {
		return (Opt<T>) none_;
	}

	public static <T> Opt<T> of(T t) {
		var p = new Opt<T>();
		p.value = t;
		return p;
	}

	public <U> Opt<U> concatMap(Fun<T, Opt<U>> fun) {
		return concatMap_(fun);
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
		return concatMap_(t -> opt1.map(u -> fun.apply(t, u)));
	}

	public T get() {
		return !isEmpty() ? value : fail("no result");
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
		return value != null ? value.toString() : "none";
	}

	private <U> Opt<U> concatMap_(Fun<T, Opt<U>> fun) {
		return !isEmpty() ? fun.apply(value) : Opt.none();
	}

}
