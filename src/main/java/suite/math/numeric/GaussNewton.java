package suite.math.numeric;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.math.sym.Symbolic;
import suite.node.Node;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.util.Array_;
import suite.util.FunUtil.Fun;

/**
 * https://en.wikipedia.org/wiki/Gauss%E2%80%93Newton_algorithm
 *
 * @author ywsing
 */
public class GaussNewton {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Matrix mtx = new Matrix();
	private Symbolic sym = new Symbolic();
	private Vector vec = new Vector();

	public float[] sym(Node[] vars, Node[] rs, float[] initials) {
		var nVars = vars.length;
		var nrs = rs.length;
		@SuppressWarnings("unchecked")
		Obj_Dbl<float[]>[] residualFuns = Array_.newArray(Obj_Dbl.class, nrs);
		Dbl_Dbl[][] gradientFuns = new Dbl_Dbl[nrs][nVars];

		for (int i = 0; i < nrs; i++) {
			Node r = rs[i];
			residualFuns[i] = sym.fun(r, vars);

			for (int j = 0; j < nVars; j++) {
				Node var = vars[j];
				gradientFuns[i][j] = sym.fun(sym.d(var, r), var);
			}
		}

		return gn(r -> {
			float[] residuals = new float[nrs];
			for (int i = 0; i < nrs; i++)
				residuals[i] = (float) residualFuns[i].apply(r);
			return residuals;
		}, betas -> {
			float[][] jacobian = new float[nrs][nVars];
			for (int i = 0; i < nrs; i++)
				for (int j = 0; j < nVars; j++)
					jacobian[i][j] = (float) gradientFuns[i][j].apply(betas[j]);
			return jacobian;
		}, initials);
	}

	/**
	 * @param residualFun
	 *            function to minimize.
	 * @param jacobianFun
	 *            Jacobian of s.
	 * @return
	 */
	private float[] gn(Fun<float[], float[]> residualFun, Fun<float[], float[][]> jacobianFun, float[] initials) {
		float[] betas = initials;

		for (int iter = 0; iter < 16; iter++) {
			float[][] j = jacobianFun.apply(betas);
			float[][] jt = mtx.transpose(j);
			vec.subOn(betas, cd.inverseMul(mtx.mul(jt, j)).apply(mtx.mul(jt, residualFun.apply(betas))));
		}

		return betas;
	}

}
