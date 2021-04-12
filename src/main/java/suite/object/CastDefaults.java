package suite.object;

import primal.adt.Opt;

public interface CastDefaults<T> {

	public default <U extends T> U cast(Class<U> clazz) {
		return clazz.isInstance(this) ? clazz.cast(this) : null;
	}

	public default <U extends T> Opt<U> castOpt(Class<U> clazz) {
		return clazz.isInstance(this) ? Opt.of(clazz.cast(this)) : Opt.none();
	}

}
