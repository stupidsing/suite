package suite.math.numeric;

import suite.math.FiniteDifference;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.primitive.DblDbl_Dbl;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.util.FunUtil.Fun;

/**
 * https://en.wikipedia.org/wiki/Broyden%E2%80%93Fletcher%E2%80%93Goldfarb%E2%80%93Shanno_algorithm
 *
 * @author ywsing
 */
public class Bfgs {

	private FiniteDifference fd = new FiniteDifference();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	// using finite differences to find gradient
	public float[] bfgs(Obj_Dbl<float[]> fun, float[] initials) {
		Fun<float[], float[]> gradientFun = fd.forward(fun);
		return bfgs(fun, gradientFun, initials);
	}

	private float[] bfgs(Obj_Dbl<float[]> fun, Fun<float[], float[]> gradientFun, float[] initials) {
		var length = initials.length;
		float[][] id = mtx.identity(length);

		float[] xs = initials;
		float[] gs = gradientFun.apply(xs);
		float[][] ib = id;

		for (int iter = 0; iter < 16; iter++) {
			float[] xs_ = xs;
			float[] ps = mtx.mul(ib, vec.neg(gs)); // direction
			Dbl_Obj<float[]> line = alpha -> vec.add(xs_, vec.scale(ps, alpha));

			double alpha = lineSearch( //
					alpha_ -> fun.apply(line.apply(alpha_)), //
					alpha_ -> vec.dot(gradientFun.apply(line.apply(alpha_)), ps), //
					1d);

			float[] ss = vec.scale(ps, alpha);
			float[] xs1 = vec.add(xs_, ss); // line.apply(alpha);
			float[] gs1 = gradientFun.apply(xs1);
			float[] ys = vec.sub(gs1, gs);
			double yts = vec.dot(ys, ss);

			float[][] ib1;

			if (alpha == 0d)
				break;
			else if (Boolean.FALSE) {
				float[][] ma = mtx.sub(id, mtx.scale(mtx.mul(ss, ys), yts));
				float[][] mb = mtx.sub(id, mtx.scale(mtx.mul(ys, ss), yts));
				ib1 = mtx.add(mtx.mul(ma, ib, mb), mtx.scale(mtx.mul(ss), yts));
			} else {
				double ytiby = vec.dot(ys, mtx.mul(ib, ys));
				float[][] ma = mtx.scale(mtx.mul(ss), yts + ytiby / (yts * yts));
				float[][] mb = mtx.scale(mtx.add(mtx.mul(ib, mtx.mul(ys, ss)), mtx.mul(mtx.mul(ss, ys), ib)), yts);
				ib1 = mtx.add(ib, mtx.sub(ma, mb));
			}

			xs = xs1;
			gs = gs1;
			ib = ib1;
		}

		return xs;
	}

	private double lineSearch(Dbl_Dbl phi, Dbl_Dbl phiGradient, double alphax) {
		double c1 = .0001d;
		double c2 = .1d;

		double alpha0 = 0d;
		double v0 = phi.apply(alpha0);
		double g0 = phiGradient.apply(alpha0);

		DblDbl_Dbl interpolate = (a0, a1) -> (a0 + a1) * .5d; // TODO
		DblDbl_Dbl choose = (a0, a1) -> (a0 + a1) * .5d; // TODO

		DblDbl_Dbl zoom = (alphaLo, alphaHi) -> {
			for (int iter = 0; iter < 16; iter++) {
				double alpha = interpolate.apply(alphaLo, alphaHi);
				double v = phi.apply(alpha);
				double g;

				if (v0 + c1 * alpha * g0 < v || phi.apply(alphaLo) <= v)
					alphaHi = alpha;
				else if (Math.abs(g = phiGradient.apply(alpha)) <= -c2 * g0)
					return alpha;
				else {
					if (0d <= g * (alphaHi - alphaLo))
						alphaHi = alphaLo;
					alphaLo = alpha;
				}
			}

			return alphaLo;
		};

		double alphap = alpha0;
		double vp = v0;
		double alpha = choose.apply(alphap, alphax);

		for (int iter = 0; iter < 16; iter++) {
			double v = phi.apply(alpha);

			if (v0 + c1 * alpha * g0 < v || 0 < iter && vp <= v)
				return zoom.apply(alphap, alpha);

			double g = phiGradient.apply(alpha);

			if (Math.abs(g) <= -c2 * g0)
				break;
			else if (0d <= g)
				return zoom.apply(alpha, alphap);
			else {
				alphap = alpha;
				vp = v;

				alpha = choose.apply(alpha, alphax);
			}
		}

		return alpha;
	}

}
