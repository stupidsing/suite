package suite.util;

import suite.util.FunUtil.Fun;

public interface AutoInterface<T> {

	public default <U extends T, V> V cast(Class<U> clazz, Fun<U, V> fun) {
		var u = cast(clazz);
		return u != null ? fun.apply(u) : null;
	}

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

	public default T self() {
		@SuppressWarnings("unchecked")
		var t = (T) this;
		return t;
	}

	public default <U> Switch<U> switch_() {
		return new Switch<>(this);
	}

}
