package suite.math.stat;

import java.util.Arrays;

import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Floats;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.Streamlet;
import suite.util.To;

public class Arima {

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();

	public LinearRegression ar(float[] ys, int n) {
		return stat.linearRegression(Ints_ //
				.range(n, ys.length) //
				.map(i -> FltObjPair.of(ys[i], Arrays.copyOfRange(ys, i - n, i))) //
				.toList());
	}

	// Digital Processing of Random Signals, Boaz Porat, page 159
	public float[] arLevinsonDurbin(float[] ys, int p) {
		double mean = stat.meanVariance(ys).mean;
		double mean2 = mean * mean;
		int length = ys.length;

		float[] r = Ints_ //
				.range(p + 1) //
				.collect(Int_Flt.lift(i -> {
					double sum = Ints_.range(i, length).toDouble(Int_Dbl.sum(j -> ys[j - i] * ys[j]));
					return (float) (sum - mean2);
				})) //
				.toArray();

		double d = r[0];
		float[] alpha = new float[p];
		alpha[0] = 1;

		for (int n = 0; n < p; n++) {
			float[] alpha0 = alpha;
			int n_ = n;

			double k1 = (1d / d) * Ints_.range(n).toDouble(Int_Dbl.sum(k -> alpha0[k] * r[n_ + 1 - k]));
			d = d * (1d - k1 * k1);

			float[] alpha1 = new float[p];
			alpha1[0] = 1f;
			for (int k = 1; k <= n; k++)
				alpha1[k] = (float) (alpha0[k] - k1 * alpha0[n + 1 - k]);
			alpha1[n + 1] = (float) -k1;
			alpha = alpha1;
		}

		// for n in 0 until p
		// K[n + 1] := d[n] ^ -1 summation(k in 0 until n, alpha[n][k] * r[n + 1
		// - k])
		// d[n + 1] := d[n] * (1 - K[n + 1] ^ 2)
		// alpha[n + 1][0] := 1
		// for k := 1 to n
		// alpha[n + 1][k] := alpha[n][k] - K[n + 1] * alpha[n][n + 1 - k]
		// alpha[n + 1][n + 1] := -K[n + 1]

		return alpha;
	}

	public float[] arch(float[] ys, int p, int q) {

		// auto regressive
		int length = ys.length;
		float[][] xs0 = To.array(length, float[].class, i -> copyPadZeroes(ys, i - p, i));
		LinearRegression lr0 = stat.linearRegression(xs0, ys, null);
		float[] variances = To.arrayOfFloats(lr0.residuals, residual -> residual * residual);

		// conditional heteroskedasticity
		LinearRegression lr1 = stat.linearRegression(Ints_ //
				.range(length) //
				.map(i -> FltObjPair.of(variances[i], copyPadZeroes(variances, i - p, i))) //
				.toList());

		return Floats_.concat(lr0.coefficients, lr1.coefficients);
	}

	public float arimaEm(float[] xs, int p, int d, int q) { // ARIMA
		for (int i = 0; i < d; i++)
			xs = ts.dropDiff(1, xs);

		float[] xs1 = Floats_.concat(xs, new float[] { armaEm(xs, p, q).x1, });
		int xLength = xs.length;

		for (int i = 0; i < d; i++) {
			int l = xLength;
			for (int j = i; j < d; j++) {
				int l0 = l;
				xs1[l0] += xs1[--l];
			}
		}

		return xs1[xLength];
	}

	// xs[t]
	// - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
	// = eps[t]
	// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
	public Arima_ armaEm(float[] xs, int p, int q) { // ARMA
		int xLength = xs.length;
		int pq = -p + q;
		int xpqLength = xLength + pq;
		float[] ars = To.arrayOfFloats(p, i -> Math.scalb(.5d, -i));
		float[] mas = To.arrayOfFloats(q, i -> Math.scalb(.5d, -i));
		float[] eps = To.arrayOfFloats(xpqLength, i -> xs[Math.max(0, Math.min(xLength, i - pq))] * .25f);

		for (int iter = 0; iter < 9; iter++) {

			// xs[t] - eps[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
			{
				float[] coeffs = stat.linearRegression(Ints_ //
						.range(p, xLength) //
						.map(t -> {
							float[] lrxs = new float[p + q];
							int tpq = t + pq;
							int k = 0;
							for (int j = 1; j <= p; j++)
								lrxs[k++] = xs[t - j];
							for (int j = 1; j <= q; j++)
								lrxs[k++] = eps[tpq - j];
							float lry = xs[t] - eps[tpq];
							return FltObjPair.of(lry, lrxs);
						}) //
						.toList()).coefficients();

				Floats_.copy(coeffs, 0, ars, 0, p);
				Floats_.copy(coeffs, p, mas, p, q);
			}

			{
				// xs[t]
				// - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
				// = eps[t] * 1
				// + eps[t - 1] * mas[0] + ... + eps[t - q] * mas[q - 1]
				Streamlet<FltObjPair<float[]>> st0 = Ints_ //
						.range(p, xLength) //
						.map(t -> {
							float[] lrxs = new float[xpqLength];
							int tq = t + pq;
							lrxs[tq--] = 1f;
							for (int j = 0; j < q; j++)
								lrxs[tq--] = mas[j];
							double lry = xs[t] - Ints_.range(p).toDouble(Int_Dbl.sum(j -> ars[j] * xs[t - j - 1]));
							return FltObjPair.of((float) lry, lrxs);
						});

				// 0 = eps[0] * eps[0] + ... + eps[t] * eps[t]
				Streamlet<FltObjPair<float[]>> st1 = Ints_ //
						.range(xpqLength) //
						.map(t -> {
							float[] lrxs = new float[xpqLength];
							lrxs[t] = eps[t];
							return FltObjPair.of(0f, lrxs);
						});

				float[] eps1 = stat.linearRegression(Streamlet.concat(st0, st1).toList()).coefficients();

				Floats_.copy(eps1, 0, eps, 0, xpqLength);
			}
		}

		// x[t]
		// = ars[0] * x[t - 1] + ... + ars[p - 1] * x[t - p]
		// + eps[t]
		// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
		// when t = xLength
		double x1 = 0d //
				+ Ints_.range(p).toDouble(Int_Dbl.sum(j -> ars[j] * xs[xLength - j - 1])) //
				+ Ints_.range(q).toDouble(Int_Dbl.sum(j -> mas[j] * eps[xpqLength - j - 1]));

		return new Arima_(ars, mas, (float) x1);
	}

	public float arimaIa(float[] xs, int p, int d, int q) { // ARIMA
		for (int i = 0; i < d; i++)
			xs = ts.dropDiff(1, xs);

		float[] xs1 = Floats_.concat(xs, new float[] { armaIa(xs, p, q).x1, });
		int xLength = xs.length;

		for (int i = 0; i < d; i++) {
			int l = xLength;
			for (int j = i; j < d; j++) {
				int l0 = l;
				xs1[l0] += xs1[--l];
			}
		}

		return xs1[xLength];
	}

	// extended from
	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// xs[t]
	// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
	// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
	// + eps[t]
	public Arima_ armaIa(float[] xs, int p, int q) {
		int length = xs.length;
		float[] eps = new float[q + length];
		int iter = 0;

		while (true) {
			int iter_ = iter;

			LinearRegression lr = stat.linearRegression(Ints_ //
					.range(length) //
					.map(t -> {
						float[] lrxs = new float[p + iter_];
						int di = 0;
						for (int i = 1; i <= p; i++)
							lrxs[di++] = xs[t - i];
						for (int i = 1; i <= q; i++)
							lrxs[di++] = eps[q + t - i];
						return FltObjPair.of(xs[t], lrxs);
					}) //
					.toList());

			if (iter < q)
				System.arraycopy(lr.residuals, 0, eps, q, length);
			else {
				float[] coeffs = lr.coefficients();
				float[] ars = Floats.of(coeffs, 0, p).toArray();
				float[] mas = Floats.of(coeffs, p).toArray();

				double x1 = 0d //
						+ Ints_.range(p).toDouble(Int_Dbl.sum(j -> ars[j] * xs[length - j - 1])) //
						+ mas[0] //
						+ Ints_.range(q).toDouble(Int_Dbl.sum(j -> mas[j] * eps[q + length - j - 1]));

				return new Arima_(ars, mas, (float) x1);
			}
		}
	}

	// Digital Processing of Random Signals, Boaz Porat, page 190
	// q << l << fs.length
	public float[] maDurbin(float[] ys, int q, int l) {
		float[] ar = arLevinsonDurbin(ys, l);
		return arLevinsonDurbin(ar, q);
	}

	// "High Frequency Trading - A Practical Guide to Algorithmic Strategies and
	// Trading Systems", Irene Aldridge, page 100
	// x[t]
	// = mas[0] * 1 + mas[1] * eps[t - 1] + ... + mas[q] * eps[t - q]
	// + eps[t]
	public float[] maIa(float[] xs, int q) {
		int length = xs.length;
		float[] eps = new float[q + length];
		int iter = 0;

		while (true) {
			int iter_ = iter;

			LinearRegression lr = stat.linearRegression(Ints_ //
					.range(length) //
					.map(t -> {
						float[] lrxs = new float[iter_ + 1];
						int di = 0;
						lrxs[di++] = 1f;
						for (int i = 1; i <= q; i++)
							lrxs[di++] = eps[q + t - i];
						return FltObjPair.of(xs[t], lrxs);
					}) //
					.toList());

			if (iter < q)
				System.arraycopy(lr.residuals, 0, eps, q, length);
			else
				return lr.coefficients();
		}
	}

	public class Arima_ {
		public final float[] ars;
		public final float[] mas;
		public final float x1;

		private Arima_(float[] ars, float[] mas, float x1) {
			this.ars = ars;
			this.mas = mas;
			this.x1 = x1;
		}
	}

	private float[] copyPadZeroes(float[] fs0, int from, int to) {
		float[] fs1 = new float[to - from];
		int p = -Math.max(0, from);
		Arrays.fill(fs1, 0, p, 0f);
		Floats_.copy(fs0, 0, fs1, p, to - p);
		return fs1;
	}

}
