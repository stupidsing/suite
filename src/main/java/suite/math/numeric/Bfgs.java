package suite.math.numeric;

import suite.primitive.DblDbl_Dbl;
import suite.primitive.Dbl_Dbl;

public class Bfgs {

	public double lineSearch(Dbl_Dbl phi, Dbl_Dbl phiGradient, double alphax) {
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
