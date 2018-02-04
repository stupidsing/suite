package suite.math;

import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class FiniteDifference {

	private double step = .001d;

	public Fun<float[], float[]> forward(Obj_Dbl<float[]> fun) {

		return xs -> {
			double ys = fun.apply(xs);
			return To.vector(xs.length, i -> {
				float x0 = xs[i];
				xs[i] += step;
				double gradient = (fun.apply(xs) - ys) / step;
				xs[i] = x0;
				return gradient;
			});
		};
	}

}
