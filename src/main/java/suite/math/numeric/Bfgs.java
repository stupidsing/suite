package suite.math.numeric;

import suite.math.FiniteDifference;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.primitive.DblDbl_Dbl;
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
		int length = initials.length;
		float[][] id = mtx.identity(length);

		float[] xs = initials;
		float[] gs = gradientFun.apply(xs);
		float[] ps = vec.neg(gs); // direction
		float[][] ib = id;

		for (int iter = 0; iter < 16; iter++) {
			float[] xs_ = xs;
			float[] ps_ = ps;
			float[] ps1 = mtx.mul(ib, vec.neg(gradientFun.apply(xs_)));

			double alpha = lineSearch( //
					alpha_ -> fun.apply(vec.add(xs_, vec.scale(ps_, alpha_))), //
					alpha_ -> vec.dot(gradientFun.apply(vec.add(xs_, vec.scale(ps_, alpha_))), ps_), //
					1d);

			float[] ss = vec.scale(ps_, alpha);
			float[] xs1 = vec.add(xs_, ss);
			float[] gs1 = gradientFun.apply(xs1);
			float[] ys = vec.sub(gs1, gs);
			double yts = vec.dot(ys, ss);

			float[][] ib1;

			if (Boolean.FALSE) {
				float[][] ma = mtx.sub(id, mtx.scale(mtx.mul(ss, ys), yts));
				float[][] mb = mtx.sub(id, mtx.scale(mtx.mul(ys, ss), yts));
				ib1 = mtx.add(mtx.mul(ma, ib, mb), mtx.scale(mtx.mul(ss), yts));
			} else {
				double sty = vec.dot(ss, ys);
				double ytiby = vec.dot(ys, mtx.mul(ib, ys));
				float[][] ma = mtx.scale(mtx.mul(ss), sty + ytiby / (sty * sty));
				float[][] mb = mtx.scale(mtx.add(mtx.mul(ib, mtx.mul(ys, ss)), mtx.mul(mtx.mul(ss, ys), ib)), sty);
				ib1 = mtx.add(ib, mtx.sub(ma, mb));
			}

			xs = xs1;
			gs = gs1;
			ps = ps1;
			ib = ib1;
		}

		return xs;
	}

	private double lineSearch(Dbl_Dbl phi, Dbl_Dbl phiGradient, double alphax) {
		double c1 = .0001d;
		double c2 = .1d;

		double alpha0 = 0d;
		double v0 = phi.apply(0d);
		double g0 = phiGradient.apply(0d);

		DblDbl_Dbl interpolate = (a0, a1) -> (a0 + a1) * .5d; // TODO
		DblDbl_Dbl choose = (a0, a1) -> (a0 + a1) * .5d; // TODO

		DblDbl_Dbl zoom = (alphaLo, alphaHi) -> {
			while (true) {
				double alpha = interpolate.apply(alphaLo, alphaHi);
				double v = phi.apply(alpha);

				if (v0 + c1 * alpha * g0 < v || phi.apply(alphaLo) <= v)
					alphaHi = alpha;
				else {
					double g = phiGradient.apply(alpha);

					if (Math.abs(g) <= -c2 * g0)
						return alpha;

					if (0d <= g * (alphaHi - alphaLo))
						alphaHi = alphaLo;

					alphaLo = alpha;
				}
			}
		};

		double alphap = alpha0;
		double vp = v0;
		double alpha = choose.apply(alphap, alphax);

		for (int iter = 0;; iter++) {
			double v = phi.apply(alpha);

			if (v0 + c1 * alpha * g0 < v || 0 < iter && vp <= v)
				return zoom.apply(alphap, alpha);

			double g = phiGradient.apply(alpha);

			if (Math.abs(g) <= -c2 * g0)
				return alpha;

			if (0d <= g)
				return zoom.apply(alpha, alphap);

			alphap = alpha;
			vp = v;

			alpha = choose.apply(alpha, alphax);
		}
	}

}
