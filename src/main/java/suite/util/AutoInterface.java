package suite.util;

import suite.adt.Opt;

public interface AutoInterface<T> {

	public default <U extends T> Opt<U> cast(Class<U> clazz) {
		return clazz.isInstance(this) ? Opt.of(clazz.cast(this)) : Opt.none();
	}

}
