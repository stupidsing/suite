package suite.util;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class Switch<R> {

	private Object in;
	private R result;

	public Switch(Object in) {
		this.in = in;
	}

	public <T> Switch<R> applyIf(Class<T> c, Fun<T, R> fun) {
		T t = c.isInstance(in) ? c.cast(in) : null;
		result = t != null ? fun.apply(t) : result;
		return this;
	}

	public <T> Switch<R> doIf(Class<T> c, Sink<T> fun) {
		return applyIf(c, t -> {
			fun.sink(t);
			@SuppressWarnings("unchecked")
			R r = (R) t;
			return r;
		});
	}

	public R nonNullResult() {
		if (result != null)
			return result;
		else
			throw new RuntimeException("cannot handle " + in);
	}

	public R result() {
		return result;
	}

}
