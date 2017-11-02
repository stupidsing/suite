package suite.util;

import suite.adt.Opt;

public interface AutoInterface {

	public default <T> Opt<T> cast(Class<T> clazz) {
		return clazz.isInstance(this) ? Opt.of(clazz.cast(this)) : Opt.none();
	}

}
