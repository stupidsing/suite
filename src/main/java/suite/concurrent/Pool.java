package suite.concurrent;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import suite.adt.Mutable;
import suite.streamlet.FunUtil.Fun;

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
		var t = Mutable.<T> nil();
		var t_ = condition.waitTill(() -> t.value() != null, () -> t.update(availables.pop()), t::value, Long.MAX_VALUE);
		var r = fun.apply(t_);
		condition.satisfyOne(() -> availables.push(t_));
		return r;
	}

}
