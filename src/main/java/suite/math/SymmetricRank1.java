package suite.math;

import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.To;

/**
 * https://en.wikipedia.org/wiki/Symmetric_rank-one
 *
 * @author ywsing
 */
public class SymmetricRank1 {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	// using finite differences to find gradient
	public float[] sr1(Obj_Dbl<float[]> fun, float[] initials) {
		double step = .001d;
		Fun<float[], float[]> gradientFun = xs -> {
			double ys = fun.apply(xs);
			return To.vector(xs.length, i -> {
				float x0 = xs[i];
				xs[i] += step;
				double gradient = (fun.apply(xs) - ys) / step;
				xs[i] = x0;
				return gradient;
			});
		};
		return sr1(fun, gradientFun, initials);
	}

	private float[] sr1(Obj_Dbl<float[]> fun, Fun<float[], float[]> gradientFun, float[] initials) {
		float[] xs = initials;
		float[] gradient = gradientFun.apply(initials);
		float[][] invh = mtx.identity(xs.length); // approximated inverse of Hessian

		for (int iter = 0; iter < 9; iter++) {
			float[] dxs = mtx.mul(invh, gradient);
			float[] xs1 = vec.add(xs, dxs);

			float[] gradient1 = gradientFun.apply(xs1);
			float[] ys = vec.sub(gradient1, gradient);
			float[] v = vec.sub(dxs, mtx.mul(invh, ys));
			float[][] mt = new float[][] { v, };
			float[][] m = mtx.transpose(mt);
			float[][] invh1 = mtx.add(invh, mtx.scale(mtx.mul(m, mt), vec.dot(v, ys)));

			xs = xs1;
			gradient = gradient1;
			invh = invh1;
		}

		return xs;
	}

}
