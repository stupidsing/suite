package suite.math.numeric;

import suite.primitive.DblPrimitives.Obj_Dbl;

public class Rosenbrock {

	public Obj_Dbl<float[]> rosenbrock(double a, double b) {
		return xy -> {
			var x = xy[0];
			var y = xy[1];
			var a_x = a - x;
			var y_x = y - x;
			return a_x * a_x + b * y_x * y_x;
		};
	};

}
