package suite.math;

import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.streamlet.FunUtil.Fun;
import suite.util.To;

public class FiniteDifference {

	private Vector vec = new Vector();
	private double step = .001d;
	private double invStep = 1d / step;

	public Fun<float[], float[]> forward(Obj_Dbl<float[]> fun) {
		return xs -> {
			var ys = fun.apply(xs);
			return To.vector(xs.length, i -> {
				var x0 = xs[i];
				xs[i] += step;
				var gradient = (fun.apply(xs) - ys) * invStep;
				xs[i] = x0;
				return gradient;
			});
		};
	}

	public Fun<float[], float[][]> forward(Fun<float[], float[]> fun) {
		return xs -> {
			var ys = fun.apply(xs);
			return To.array(xs.length, float[].class, i -> {
				var x0 = xs[i];
				xs[i] += step;
				var gradient = vec.scale(vec.sub(fun.apply(xs), ys), invStep);
				xs[i] = x0;
				return gradient;
			});
		};
	}

}
