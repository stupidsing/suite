package suite.streamlet;

import primal.fp.Funs.Fun;
import primal.primitive.DblPrim.Obj_Dbl;
import primal.primitive.IntPrim.Obj_Int;

public interface StreamletDefaults<T, Puller_ extends PullerDefaults<T>> extends Iterable<T> {

	public Puller_ puller();

	public default <R> R collect(Fun<Puller_, R> fun) {
		return fun.apply(puller());
	}

	public default int size() {
		return puller().count();
	}

	public default double toDouble(Obj_Dbl<Puller_> fun) {
		return fun.apply(puller());
	}

	public default int toInt(Obj_Int<Puller_> fun) {
		return fun.apply(puller());
	}

}
