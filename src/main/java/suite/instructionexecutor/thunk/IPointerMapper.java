package suite.instructionexecutor.thunk;

import suite.persistent.PerPointer;
import suite.streamlet.FunUtil.Fun;

public class IPointerMapper<T0, T1> {

	private Fun<T0, T1> fun;

	private IPointerMapper(Fun<T0, T1> fun) {
		this.fun = fun;
	}

	public static <T0, T1> PerPointer<T1> map(Fun<T0, T1> fun, PerPointer<T0> pointer) {
		return new IPointerMapper<>(fun).map(pointer);
	}

	public PerPointer<T1> map(PerPointer<T0> pointer) {
		return new PerPointer<>() {
			public T1 head() {
				var head = pointer.head();
				return head != null ? fun.apply(head) : null;
			}

			public PerPointer<T1> tail() {
				return map(pointer.tail());
			}
		};
	}

}
