package suite.instructionexecutor.thunk;

import suite.immutable.IPointer;
import suite.streamlet.FunUtil.Fun;

public class IPointerMapper<T0, T1> {

	private Fun<T0, T1> fun;

	private IPointerMapper(Fun<T0, T1> fun) {
		this.fun = fun;
	}

	public static <T0, T1> IPointer<T1> map(Fun<T0, T1> fun, IPointer<T0> pointer) {
		return new IPointerMapper<>(fun).map(pointer);
	}

	public IPointer<T1> map(IPointer<T0> pointer) {
		return new IPointer<>() {
			public T1 head() {
				var head = pointer.head();
				return head != null ? fun.apply(head) : null;
			}

			public IPointer<T1> tail() {
				return map(pointer.tail());
			}
		};
	}

}
