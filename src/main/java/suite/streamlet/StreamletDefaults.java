package suite.streamlet;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.FunUtil.Fun;

public interface StreamletDefaults<T, Outlet_ extends OutletDefaults<T>> extends Iterable<T> {

	public Outlet_ outlet();

	public default <R> R collect(Fun<Outlet_, R> fun) {
		return fun.apply(outlet());
	}

	public default int size() {
		return outlet().count();
	}

	public default double toDouble(Obj_Dbl<Outlet_> fun) {
		return fun.apply(outlet());
	}

	public default int toInt(Obj_Int<Outlet_> fun) {
		return fun.apply(outlet());
	}

}
