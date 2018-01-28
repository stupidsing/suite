package suite.editor;

import java.util.Objects;

import suite.streamlet.Signal;

public class ValueSignal<T> {

	private T value;
	public final Signal<T> changed = Signal.of();

	public static <T> ValueSignal<T> of(T value) {
		return new ValueSignal<>(value);
	}

	private ValueSignal(T value_) {
		value = value_;
	}

	public void change(T value_) {
		if (!Objects.equals(value, value_)) {
			value = value_;
			changed.fire(value_);
		}
	}

	public T get() {
		return value;
	}

}
