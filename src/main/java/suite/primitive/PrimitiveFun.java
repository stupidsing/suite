package suite.primitive;

import suite.util.FunUtil2.Fun2;

public class PrimitiveFun {

	@FunctionalInterface
	public interface ObjObj_Obj<X, Y, Z> extends Fun2<X, Y, Z> {
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
