package suite.util;

import suite.primitive.IoSink;
import suite.util.FunUtil.Fun;

public class Switch<R> {

	private Object in;
	private R result;

	public Switch(Object in) {
		this.in = in;
	}

	public <T> Switch<R> applyIf(Class<T> c, Fun<T, R> fun) {
		if (result == null && c.isInstance(in))
			result = fun.apply(c.cast(in));
		return this;
	}

	public <T> Switch<R> doIf(Class<T> c, IoSink<T> fun) {
		return applyIf(c, t -> {
			@SuppressWarnings("unchecked")
			var r = (R) Rethrow.ex(() -> {
				fun.sink(t);
				return t;
			});
			return r;
		});
	}

	public R nonNullResult() {
		return result != null ? result : Fail.t("cannot handle " + in);
	}

	public R result() {
		return result;
	}

}
