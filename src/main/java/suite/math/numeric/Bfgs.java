package suite.math.numeric;

import static java.lang.Math.abs;

import primal.fp.Funs.Fun;
import primal.primitive.DblDbl_Dbl;
import suite.math.FiniteDifference;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;

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
		var gradientFun = fd.forward(fun);
		return bfgs(fun, gradientFun, initials);
	}

	private float[] bfgs(Obj_Dbl<float[]> fun, Fun<float[], float[]> gradientFun, float[] initials) {
		var length = initials.length;
		var id = mtx.identity(length);

		var xs = initials;
		var gs = gradientFun.apply(xs);
		var ib = id;

		for (var iter = 0; iter < 16; iter++) {
			var xs_ = xs;
			var ps = mtx.mul(ib, vec.neg(gs)); // direction
			Dbl_Obj<float[]> line = alpha -> vec.add(xs_, vec.scale(ps, alpha));

			var alpha = lineSearch( //
					alpha_ -> fun.apply(line.apply(alpha_)), //
					alpha_ -> vec.dot(gradientFun.apply(line.apply(alpha_)), ps), //
					1d);

			var ss = vec.scale(ps, alpha);
			var xs1 = vec.add(xs_, ss); // line.apply(alpha);
			var gs1 = gradientFun.apply(xs1);
			var ys = vec.sub(gs1, gs);
			var yts = vec.dot(ys, ss);

			float[][] ib1;

			if (alpha == 0d)
				break;
			else if (Boolean.FALSE) {
				var ma = mtx.sub(id, mtx.scale(mtx.mul(ss, ys), yts));
				var mb = mtx.sub(id, mtx.scale(mtx.mul(ys, ss), yts));
				ib1 = mtx.add(mtx.mul(ma, ib, mb), mtx.scale(mtx.mul(ss), yts));
			} else {
				var ytiby = vec.dot(ys, mtx.mul(ib, ys));
				var ma = mtx.scale(mtx.mul(ss), yts + ytiby / (yts * yts));
				var mb = mtx.scale(mtx.add(mtx.mul(ib, mtx.mul(ys, ss)), mtx.mul(mtx.mul(ss, ys), ib)), yts);
				ib1 = mtx.add(ib, mtx.sub(ma, mb));
			}

			xs = xs1;
			gs = gs1;
			ib = ib1;
		}

		return xs;
	}

	private double lineSearch(Dbl_Dbl phi, Dbl_Dbl phiGradient, double alphax) {
		var c1 = .0001d;
		var c2 = .1d;

		var alpha0 = 0d;
		var v0 = phi.apply(alpha0);
		var g0 = phiGradient.apply(alpha0);

		DblDbl_Dbl interpolate = (a0, a1) -> (a0 + a1) * .5d; // TODO
		DblDbl_Dbl choose = (a0, a1) -> (a0 + a1) * .5d; // TODO

		DblDbl_Dbl zoom = (alphaLo, alphaHi) -> {
			for (var iter = 0; iter < 16; iter++) {
				var alpha = interpolate.apply(alphaLo, alphaHi);
				var v = phi.apply(alpha);
				double g;

				if (v0 + c1 * alpha * g0 < v || phi.apply(alphaLo) <= v)
					alphaHi = alpha;
				else if (abs(g = phiGradient.apply(alpha)) <= -c2 * g0)
					return alpha;
				else {
					if (0d <= g * (alphaHi - alphaLo))
						alphaHi = alphaLo;
					alphaLo = alpha;
				}
			}

			return alphaLo;
		};

		var alphap = alpha0;
		var vp = v0;
		var alpha = choose.apply(alphap, alphax);

		for (var iter = 0; iter < 16; iter++) {
			var v = phi.apply(alpha);

			if (v0 + c1 * alpha * g0 < v || 0 < iter && vp <= v)
				return zoom.apply(alphap, alpha);

			var g = phiGradient.apply(alpha);

			if (abs(g) <= -c2 * g0)
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
