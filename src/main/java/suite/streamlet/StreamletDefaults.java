package suite.streamlet;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntPrimitives.Obj_Int;

public interface StreamletDefaults<T, Outlet_> extends Iterable<T> {

	public Outlet_ outlet();

	public default double collectAsDouble(Obj_Dbl<Outlet_> fun) {
		return fun.apply(outlet());
	}

	public default int collectAsInt(Obj_Int<Outlet_> fun) {
		return fun.apply(outlet());
	}

}
