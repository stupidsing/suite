package suite.instructionexecutor.thunk;

import suite.immutable.IPointer;
import suite.util.FunUtil.Fun;

public class IPointerMapper<T0, T1> {

	private Fun<T0, T1> fun;

	public IPointerMapper(Fun<T0, T1> fun) {
		this.fun = fun;
	}

	public IPointer<T1> map(IPointer<T0> pointer) {
		return new IPointer<T1>() {
			public T1 head() {
				T0 head = pointer.head();
				return head != null ? fun.apply(head) : null;
			}

			public IPointer<T1> tail() {
				return map(pointer.tail());
			}
		};
	}

}
