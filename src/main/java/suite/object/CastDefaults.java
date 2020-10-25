package suite.object;

import primal.fp.Funs.Fun;

public interface CastDefaults<T> {

	public default <U extends T, V> V cast(Class<U> clazz, Fun<U, V> fun) {
		var u = cast(clazz);
		return u != null ? fun.apply(u) : null;
	}

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

}
