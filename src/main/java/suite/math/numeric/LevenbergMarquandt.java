package suite.math.numeric;

import suite.math.FiniteDifference;
import suite.math.linalg.GaussSeidel;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.streamlet.FunUtil.Fun;

/**
 * https://en.wikipedia.org/wiki/Levenberg%E2%80%93Marquardt_algorithm
 *
 * @author ywsing
 */
public class LevenbergMarquandt {

	private FiniteDifference fd = new FiniteDifference();
	private GaussSeidel gs = new GaussSeidel();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	// using finite differences to find gradient
	public float[] lm(float[] xs, float[] ys, Fun<float[], float[]> fun, float[] betas) {
		var gradientFun = fd.forward(fun);
		return lm(xs, ys, fun, gradientFun, betas);
	}

	private float[] lm(float[] xs, float[] ys, Fun<float[], float[]> fun, Fun<float[], float[][]> gradientFun, float[] betas) {
		var lambda = 1d;

		for (int iter = 0; iter < 16; iter++) {
			var lambda1 = lambda + 1;
			var js = gradientFun.apply(xs);
			var jjs = mtx.mul_mnT(js, js);

			for (var i = 0; i < jjs.length; i++)
				jjs[i][i] *= lambda1;

			var ds = vec.sub(ys, fun.apply(betas));
			var rs = gs.solve(jjs, ds);
			betas = vec.add(betas, rs);
		}

		return betas;
	}

}
