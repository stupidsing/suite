package suite.math.numeric;

import suite.math.FiniteDifference;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.util.FunUtil.Fun;

/**
 * https://en.wikipedia.org/wiki/Symmetric_rank-one
 *
 * @author ywsing
 */
public class SymmetricRank1 {

	private FiniteDifference fd = new FiniteDifference();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	// using finite differences to find gradient
	public float[] sr1(Obj_Dbl<float[]> fun, float[] initials) {
		Fun<float[], float[]> gradientFun = fd.forward(fun);
		return sr1(fun, gradientFun, initials);
	}

	private float[] sr1(Obj_Dbl<float[]> fun, Fun<float[], float[]> gradientFun, float[] initials) {
		var xs = initials;
		var gradient = gradientFun.apply(initials);
		float[][] invh = mtx.identity(xs.length); // approximated inverse of Hessian

		for (int iter = 0; iter < 9; iter++) {
			float[] dxs = mtx.mul(invh, gradient);
			float[] xs1 = vec.add(xs, dxs);

			var gradient1 = gradientFun.apply(xs1);
			float[] ys = vec.sub(gradient1, gradient);
			float[] v = vec.sub(dxs, mtx.mul(invh, ys));
			float[][] invh1 = mtx.add(invh, mtx.scale(mtx.mul(v), vec.dot(v, ys)));

			xs = xs1;
			gradient = gradient1;
			invh = invh1;
		}

		return xs;
	}

}
