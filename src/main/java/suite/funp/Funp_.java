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

	public static <T, R> R applyIf(Object in, Class<T> c, R result, Fun<T, R> fun) {
		T t = c.isInstance(in) ? c.cast(in) : null;
		if (t != null)
			result = fun.apply(t);
		return result;
	}

}
