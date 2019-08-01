package suite.concurrent;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import primal.adt.Mutable;
import primal.fp.Funs.Fun;

public class Pool<T> {

	private Deque<T> availables = new ArrayDeque<>();
	private Condition condition = new Condition();

	@SafeVarargs
	public static <T> Pool<T> of(T... ts) {
		var pool = new Pool<T>();
		pool.availables.addAll(Arrays.<T> asList(ts));
		return pool;
	}

	public <R> R get(Fun<T, R> fun) {
		var t_ = get();
		var r = fun.apply(t_);
		unget(t_);
		return r;
	}

	public T get() {
		var mut = Mutable.<T> nil();
		return condition.waitTill(() -> mut.value() != null, () -> mut.update(availables.pop()), mut::value, Long.MAX_VALUE);
	}

	public void unget(T t) {
		condition.satisfyOne(() -> availables.push(t));
	}

}
