package suite.math;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.util.FunUtil.Fun;

/**
 * https://en.wikipedia.org/wiki/Gauss%E2%80%93Newton_algorithm
 *
 * @author ywsing
 */
public class GaussNewton {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	/**
	 * @param sfun
	 *            function to minimize.
	 * @param jfun
	 *            Jacobian of s.
	 * @return
	 */
	public float[] gn(Fun<float[], float[]> rfun, Fun<float[], float[][]> jfun, float[] initials) {
		float[] beta = initials;

		for (int iter = 0; iter < 16; iter++) {
			float[][] j = jfun.apply(beta);
			float[][] jt = mtx.transpose(j);
			vec.subOn(beta, cd.inverseMul(mtx.mul(jt, j)).apply(mtx.mul(jt, rfun.apply(beta))));
		}

		return beta;
	}

}
