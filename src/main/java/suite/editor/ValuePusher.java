package suite.editor;

import java.util.Objects;

import suite.streamlet.Pusher;

public class ValuePusher<T> {

	private T value;
	public final Pusher<T> changed = Pusher.of();

	public static <T> ValuePusher<T> of(T value) {
		return new ValuePusher<>(value);
	}

	private ValuePusher(T value_) {
		value = value_;
	}

	public void change(T value_) {
		if (!Objects.equals(value, value_)) {
			value = value_;
			changed.push(value_);
		}
	}

	public T get() {
		return value;
	}

}
