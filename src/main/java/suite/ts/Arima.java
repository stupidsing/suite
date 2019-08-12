package suite.ts;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.scalb;
import static suite.util.Streamlet_.forInt;

import java.util.Arrays;
import java.util.Random;

import primal.primitive.DblPrim.DblSource;
import primal.primitive.FltMoreVerbs.ConcatFlt;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.FltVerbs.CopyFlt;
import primal.primitive.adt.Floats;
import primal.primitive.adt.pair.DblObjPair;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.fp.AsFlt;
import suite.math.linalg.Vector;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.streamlet.As;
import suite.util.To;

public class Arima {

	private Mle mle = new Mle();
	private Statistic stat = new Statistic();
	private Random random = new Random();
	private TimeSeries ts = new TimeSeries();
	private Vector vec = new Vector();

	public LinearRegression ar(float[] ys, int n) {
		return stat.linearRegression(forInt(n, ys.length).map(i -> FltObjPair.of(ys[i], Arrays.copyOfRange(ys, i - n, i))));
	}

	// Digital Processing of Random Signals, Boaz Porat, page 159
	private float[] arLevinsonDurbin(float[] ys, int p) {
		var mean = stat.meanVariance(ys).mean;
		var mean2 = mean * mean;
		var length = ys.length;

		var r = forInt(p + 1) //
				.collect(As.floats(i -> {
					var sum = forInt(i, length).toDouble(As.sum(j -> ys[j - i] * ys[j]));
					return (float) (sum - mean2);
				})) //
				.toArray();

		var d = (double) r[0];
		var alpha = new float[p];
		alpha[0] = 1;

		for (var n = 0; n < p; n++) {
			var alpha0 = alpha;
			var n_ = n;

			var k1 = forInt(n).toDouble(As.sum(k -> alpha0[k] * r[n_ + 1 - k])) / d;
			d = d * (1d - k1 * k1);

			var alpha1 = new float[p];
			alpha1[0] = 1f;
			for (var k = 1; k <= n; k++)
				alpha1[k] = (float) (alpha0[k] - k1 * alpha0[n + 1 - k]);
			alpha1[n + 1] = (float) -k1;
			alpha = alpha1;
		}

		// for n in 0 until p
		// K[n + 1] := d[n] ^ -1 * summation(0 <= k < n, alpha[n][k] * r[n + 1 - k])
		// d[n + 1] := d[n] * (1 - K[n + 1] ^ 2)
		// alpha[n + 1][0] := 1
		// for k := 1 to n
		// alpha[n + 1][k] := alpha[n][k] - K[n + 1] * alpha[n][n + 1 - k]
		// alpha[n + 1][n + 1] := -K[n + 1]

		return alpha;
	}

	public DblObjPair<Arima_> arimaBackcast(float[] xs0, int p, int d, int q) {
		var ars = To.vector(p, i -> 1f);
		var mas = To.vector(q, i -> 1f);
		var xs1 = nDiffs(xs0, d);
		var arima = armaBackcast(xs1, ars, mas);
		var xs2 = AsFlt.concat(xs1, vec.of(arima.x1));
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// http://math.unice.fr/~frapetti/CorsoP/Chapitre_4_IMEA_1.pdf
	// "Least squares estimation using backcasting procedure"
	public Arima_ armaBackcast(float[] xs, float[] ars, float[] mas) {
		var length = xs.length;
		var p = ars.length;
		var q = mas.length;
		var xsp = AsFlt.concat(new float[p], xs);
		var epq = new float[length + q];
		var arma = new Arma(ars, mas);

		for (var iter = 0; iter < 64; iter++) {

			// backcast
			// ep[t]
			// = (xs[t + q] - ep[t + q]
			// - ars[0] * xs[t + q - 1] - ... - ars[p - 1] * xs[t + q - p]
			// - mas[0] * ep[t + q - 1] - ... - mas[q - 2] * ep[t + 1]
			// ) / mas[q - 1]
			arma.backcast(xsp, epq);

			// forward recursion
			// ep[t] = xs[t]
			// - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
			// - mas[0] * ep[t - 1] - ... - mas[q - 1] * ep[t - q]
			var error = arma.forwardRecursion(xsp, epq);

			// minimization
			// xs[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
			// + ep[t]
			var lr = stat.linearRegression(forInt(length) //
					.map(t -> {
						int tp = t + p, tpm1 = tp - 1;
						int tq = t + q, tqm1 = tq - 1;
						var lrxs0 = forInt(p).collect(As.floats(i -> xsp[tpm1 - i]));
						var lrxs1 = forInt(q).collect(As.floats(i -> epq[tqm1 - i]));
						return FltObjPair.of(xsp[tp], ConcatFlt.of(lrxs0, lrxs1).toArray());
					}));

			System.out.println("iter " + iter + ", error = " + To.string(error) + lr);
			System.out.println();

			var coefficients = lr.coefficients();
			CopyFlt.array(coefficients, 0, ars, 0, p);
			CopyFlt.array(coefficients, p, mas, 0, q);
		}

		var x1 = arma.sum(xsp, epq);
		return new Arima_(ars, mas, x1);
	}

	@SuppressWarnings("unused")
	private DblObjPair<Arima_> arimaEm(float[] xs0, int p, int d, int q) {
		var xs1 = nDiffs(xs0, d);
		var arima = armaEm(xs1, p, q);
		var xs2 = AsFlt.concat(xs1, vec.of(arima.x1));
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// xs[t] - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
	// = ep[t] + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
	private Arima_ armaEm(float[] xs, int p, int q) { // ARMA
		var length = xs.length;
		var lengthp = length + p;
		var lengthq = length + q;
		var ars = To.vector(p, i -> scalb(.5d, -i));
		var mas = To.vector(q, i -> scalb(.5d, -i));
		var xsp = new float[lengthp];
		var epq = To.vector(lengthq, i -> xs[max(0, min(xsp.length, i - q))] * .25f);

		Arrays.fill(xsp, 0, p, xs[0]);
		CopyFlt.array(xs, 0, xsp, p, length);

		for (var iter = 0; iter < 9; iter++) {

			// xs[t] - ep[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
			{
				var coeffs = stat.linearRegression(forInt(length) //
						.map(t -> {
							var tp = t + p;
							var tq = t + q;
							var lrxs = ConcatFlt //
									.of(Floats_.reverse(xsp, t, tp), Floats_.reverse(epq, t, tq)) //
									.toArray();
							var lry = xsp[tp] - epq[tq];
							return FltObjPair.of(lry, lrxs);
						})).coefficients();

				CopyFlt.array(coeffs, 0, ars, 0, p);
				CopyFlt.array(coeffs, p, mas, p, q);
			}

			{
				// xs[t] - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
				// = ep[t] + ep[t - 1] * mas[0] + ... + ep[t - q] * mas[q - 1]
				var epq1 = stat.linearRegression(forInt(length) //
						.map(t -> {
							var lrxs = new float[lengthq];
							var tp = t + p;
							var tq = t + q;
							lrxs[tq--] = 1f;
							for (var i = 0; i < q; i++)
								lrxs[tq--] = mas[i];
							var lry = xsp[tp] - vec.convolute(p, ars, xsp, tp);
							return FltObjPair.of((float) lry, lrxs);
						})).coefficients();

				CopyFlt.array(epq1, 0, epq, 0, lengthq);
			}
		}

		// xs[t]
		// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
		// + ep[t]
		// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
		// when t = xs.length
		var x1 = new Arma(ars, mas).sum(xsp, epq);

		return new Arima_(ars, mas, x1);
	}

	@SuppressWarnings("unused")
	private DblObjPair<Arima_> arimaIa(float[] xs0, int p, int d, int q) {
		var xs1 = nDiffs(xs0, d);
		var arima = armaIa(xs1, p, q);
		var xs2 = AsFlt.concat(xs1, vec.of(arima.x1));
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	// extended from
	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// xs[t]
	// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
	// + ep[t]
	// + mas[0] * ep[t - 1] + ... + mas[q - 1] * ep[t - q]
	private Arima_ armaIa(float[] xs, int p, int q) {
		var length = xs.length;
		int lengthp = length + p, lengthpm1 = lengthp - 1;
		int lengthq = length + q, lengthqm1 = lengthq - 1;
		var iter = 0;
		var xsp = new float[lengthp];
		var epqByIter = new float[q][];

		Arrays.fill(xsp, 0, p, xs[0]);
		CopyFlt.array(xs, 0, xsp, p, length);

		while (true) {
			var iter_ = iter;

			var lr = stat.linearRegression(forInt(length) //
					.map(t -> {
						var tp = t + p;
						int tq = t + q, tqm1 = tq - 1;
						var lrxs = ConcatFlt //
								.of(Floats_.reverse(xsp, t, tp),
										forInt(iter_).collect(As.floats(i -> epqByIter[i][tqm1 - i]))) //
								.toArray();
						return FltObjPair.of(xsp[tp], lrxs);
					}));

			var coeffs = lr.coefficients();

			if (iter < q)
				CopyFlt.array(lr.residuals, 0, epqByIter[iter++] = new float[lengthq], q, length);
			else {
				var ars = Floats.of(coeffs, 0, p).toArray();
				var mas = Floats.of(coeffs, p).toArray();

				var x1 = 0d //
						+ forInt(p).toDouble(As.sum(i -> ars[i] * xsp[lengthpm1 - i])) //
						+ forInt(q).toDouble(As.sum(i -> mas[i] * epqByIter[i][lengthqm1 - i]));

				return new Arima_(ars, mas, x1);
			}
		}
	}

	public DblObjPair<Arima_> arimaMle(float[] xs0, int p, int d, int q) {
		var xs1 = nDiffs(xs0, d);
		var arima = armaMle(xs1, p, q);
		var xs2 = AsFlt.concat(xs1, vec.of(arima.x1));
		return DblObjPair.of(nSums(xs2, d), arima);
	}

	private Arima_ armaMle(float[] xs, int p, int q) {
		var length = xs.length;
		var xsp = AsFlt.concat(new float[p], xs);
		var epq = new float[length + q];

		class LogLikelihood implements DblSource {
			private float[] ars = To.vector(p, i -> random.nextGaussian());
			private float[] mas = To.vector(q, i -> random.nextGaussian());
			private Arma arma = new Arma(ars, mas);

			public double g() {
				arma.backcast(xsp, epq);
				return -arma.forwardRecursion(xsp, epq);
			}
		}

		var ll = mle.max(LogLikelihood::new);
		var ars = ll.ars;
		var mas = ll.mas;

		var x1 = ll.arma.sum(xsp, epq);
		return new Arima_(ars, mas, x1);
	}

	// Digital Processing of Random Signals, Boaz Porat, page 190
	// q << l << fs.length
	@SuppressWarnings("unused")
	private float[] maDurbin(float[] ys, int q, int l) {
		var ar = arLevinsonDurbin(ys, l);
		return arLevinsonDurbin(ar, q);
	}

	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// xs[t]
	// = mas[0] * 1 + mas[1] * ep[t - 1] + ... + mas[q] * ep[t - q]
	// + ep[t]
	@SuppressWarnings("unused")
	private float[] maIa(float[] xs, int q) {
		var length = xs.length;
		var epqByIter = new float[q][];
		var iter = 0;
		var qm1 = q - 1;

		while (true) {
			var iter_ = iter;

			var lr = stat.linearRegression(forInt(length) //
					.map(t -> {
						var tqm1 = t + qm1;
						var lrxs = ConcatFlt //
								.of(ReadFlt.from(1f), forInt(iter_).collect(As.floats(i -> epqByIter[i][tqm1 - i]))) //
								.toArray();
						return FltObjPair.of(xs[t], lrxs);
					}));

			if (iter < q)
				CopyFlt.array(lr.residuals, 0, epqByIter[iter++] = new float[q + length], q, length);
			else
				return lr.coefficients();
		}
	}

	public class Arima_ {
		public final float[] ars;
		public final float[] mas;
		public final double x1;

		private Arima_(float[] ars, float[] mas, double x1) {
			this.ars = ars;
			this.mas = mas;
			this.x1 = x1;
		}
	}

	private class Arma {
		private int p, q;
		private float[] ars;
		private float[] mas;

		private Arma(float[] ars, float[] mas) {
			p = ars.length;
			q = mas.length;
			this.ars = ars;
			this.mas = mas;
		}

		private void backcast(float[] xsp, float[] epq) {
			var qm1 = q - 1;

			for (var t = qm1; 0 <= t; t--) {
				var sum = sum(xsp, epq, t, p, qm1);
				epq[t] = (float) ((xsp[t + p] - epq[t + q] - sum) / mas[qm1]);
			}
		}

		private double forwardRecursion(float[] xsp, float[] epq) {
			var length = xsp.length - p;
			var error = 0d;

			for (var t = 0; t < length; t++) {
				var tp = t + p;
				var tq = t + q;
				var ep = xsp[tp] - sum(xsp, epq, t, p, q);
				epq[tq] = (float) ep;
				error += ep * ep;
			}

			return error;
		}

		private double sum(float[] xsp, float[] epq) {
			return sum(xsp, epq, xsp.length - p, p, q);
		}

		private double sum(float[] xsp, float[] epq, int t, int p_, int q_) {
			return vec.convolute(p_, ars, xsp, t + p) + vec.convolute(q_, mas, epq, t + q);
		}
	}

	private float[] nDiffs(float[] xs, int d) {
		for (var i = 0; i < d; i++)
			xs = ts.dropDiff(1, xs);
		return xs;
	}

	private float nSums(float[] xs, int d) {
		var lengthm1 = xs.length - 1;
		for (var i = 0; i < d; i++) {
			var l = lengthm1;
			for (var j = i; j < d; j++) {
				var l0 = l;
				xs[l0] += xs[--l];
			}
		}
		return xs[lengthm1];
	}

}
