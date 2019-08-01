package suite.editor;

import primal.Verbs.Equals;
import suite.streamlet.Pusher;

public class ValuePusher<T> {

	private T value;
	public final Pusher<T> changed = new Pusher<>();

	public static <T> ValuePusher<T> of(T value) {
		return new ValuePusher<>(value);
	}

	private ValuePusher(T value_) {
		value = value_;
	}

	public void change(T value_) {
		if (!Equals.ab(value, value_)) {
			value = value_;
			changed.push(value_);
		}
	}

	public T get() {
		return value;
	}

}
