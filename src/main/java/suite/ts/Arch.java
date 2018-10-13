package suite.ts;

import static suite.util.Friends.forInt;
import static suite.util.Friends.log;
import static suite.util.Friends.max;

import java.util.Arrays;
import java.util.Random;

import suite.math.numeric.Statistic;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.To;

public class Arch {

	private Mle mle = new Mle();
	private Statistic stat = new Statistic();
	private Random random = new Random();

	public float[] arch(float[] ys, int p, int q) {

		// auto regressive
		var length = ys.length;
		var xs0 = To.array(length, float[].class, i -> copyPadZeroes(ys, i - p, i));
		var lr0 = stat.linearRegression(xs0, ys, null);
		var variances = To.vector(lr0.residuals, residual -> residual * residual);

		// conditional heteroskedasticity
		var lr1 = stat.linearRegression(Ints_ //
				.for_(length) //
				.map(i -> FltObjPair.of(variances[i], copyPadZeroes(variances, i - p, i))));

		return Floats_.concat(lr0.coefficients, lr1.coefficients);
	}

	// https://quant.stackexchange.com/questions/9351/algorithm-to-fit-ar1-garch1-1-model-of-log-returns
	public Object[] garchp1(float[] xs, int p) {
		class LogLikelihood implements DblSource {
			private double c = random.nextDouble() * .0001d;
			private float[] ars = To.vector(p, i -> random.nextDouble() * .01d);
			private double p0 = random.nextDouble() * .00002d;
			private double p1 = random.nextDouble() * .001d;
			private double p2 = .9d + random.nextDouble() * .001d;

			public double source() {
				var eps = 0d;
				var var = 0d;
				var logLikelihood = 0d;

				for (var t = p; t < xs.length; t++) {
					var tm1 = t - 1;
					var eps0 = eps;
					var var0 = var;
					var estx = c + forInt(p).toDouble(Int_Dbl.sum(i -> ars[i] * xs[tm1 - i]));
					eps = xs[t] - estx;
					var = p0 + p1 * eps0 * eps0 + p2 * var0;
					logLikelihood += -.5d * (log(var) + eps * eps / var);
				}

				return logLikelihood;
			}
		}

		var ll = mle.max(LogLikelihood::new);
		return new Object[] { ll.c, ll.ars, ll.p0, ll.p1, ll.p2, };
	}

	private float[] copyPadZeroes(float[] fs0, int from, int to) {
		var fs1 = new float[to - from];
		var p = -max(0, from);
		Arrays.fill(fs1, 0, p, 0f);
		Floats_.copy(fs0, 0, fs1, p, to - p);
		return fs1;
	}

}
