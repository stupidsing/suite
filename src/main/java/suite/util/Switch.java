package suite.util;

import suite.util.FunUtil.Fun;

public class Switch<R> {

	private Object in;
	private R result;

	public Switch(Object in) {
		this.in = in;
	}

	public <T> void applyIf(Class<T> c, Fun<T, R> fun) {
		T t = c.isInstance(in) ? c.cast(in) : null;
		result = t != null ? fun.apply(t) : result;
	}

	public R result() {
		return result;
	}

}
