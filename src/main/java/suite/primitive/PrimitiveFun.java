package suite.primitive;

import java.util.function.BiFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface ObjObj_Obj<X, Y, Z> extends BiFunction<X, Y, Z> {
		public default ObjObj_Obj<X, Y, Z> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					throw new RuntimeException("for " + x + ", " + y, ex);
				}
			};
		}
	}

}
