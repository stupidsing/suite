package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import primal.fp.Funs.Fun;
import suite.primitive.IoSink;

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
			var r = (R) ex(() -> {
				fun.f(t);
				return t;
			});
			return r;
		});
	}

	public R nonNullResult() {
		return result != null ? result : fail("cannot handle " + in);
	}

	public R result() {
		return result;
	}

}
