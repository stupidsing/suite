package suite.object;

import suite.util.Switch;

public interface SwitchDefaults<T> {

	public default Switch<T> sw() {
		return switch_();
	}

	public default <U> Switch<U> switch_() {
		return new Switch<>(this);
	}

}
