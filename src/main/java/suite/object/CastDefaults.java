package suite.object;

import primal.fp.Funs.Fun;

public interface CastDefaults<T> {

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

	public default <U extends T, V> V castMap(Class<U> clazz, Fun<U, V> fun) {
		return clazz.isInstance(this) ? fun.apply(clazz.cast(this)) : null;
	}

}
