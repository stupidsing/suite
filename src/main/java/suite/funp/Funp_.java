package suite.funp;

import suite.funp.P1.FunpFramePointer;
import suite.util.FunUtil.Fun;

public class Funp_ {

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;

	public static FunpFramePointer framePointer = new FunpFramePointer();

	public interface Funp {
	}

	public static class Switch<R> {
		private Object in;
		private R result;

		public Switch(Object in) {
			this.in = in;
		}

		public <T> void applyIf(Class<T> c, Fun<T, R> fun) {
			T t = c.isInstance(in) ? c.cast(in) : null;
			result = t != null ? fun.apply(t) : result;
		}

		public R result() {
			return result;
		}
	}

}
