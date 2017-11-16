package suite.math.stat;

import java.util.Arrays;

import suite.math.linalg.Vector_;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.Ints_;
import suite.util.To;

public class Arima {

	private Statistic stat = new Statistic();
	private TimeSeries ts = new TimeSeries();
	private Vector_ vec = new Vector_();

	public LinearRegression ar(float[] ys, int n) {
		int length = ys.length;
		float[][] deps = To.array(length - n, float[].class, i -> Arrays.copyOfRange(ys, i, i + n));
		float[] ys1 = Arrays.copyOfRange(ys, n, length);
		return stat.linearRegression(deps, ys1);
	}

	// Digital Processing of Random Signals, Boaz Porat, page 159
	public float[] arLevinsonDurbin(float[] ys, int p) {
		double mean = stat.meanVariance(ys).mean;
		double mean2 = mean * mean;
		int length = ys.length;

		float[] r = Ints_ //
				.range(p + 1) //
				.collect(Int_Flt.lift(i -> {
					double sum = Ints_.range(i, length).collectAsDouble(Int_Dbl.sum(j -> ys[j - i] * ys[j]));
					return (float) (sum - mean2);
				})) //
				.toArray();

		double d = r[0];
		float[] alpha = new float[p];
		alpha[0] = 1;

		for (int n = 0; n < p; n++) {
			float[] alpha0 = alpha;
			int n_ = n;

			double k1 = (1d / d) * Ints_.range(n).collectAsDouble(Int_Dbl.sum(k -> alpha0[k] * r[n_ + 1 - k]));
			d = d * (1d - k1 * k1);

			float[] alpha1 = new float[p];
			alpha1[0] = 1f;
			for (int k = 1; k <= n; k++)
				alpha1[k] = (float) (alpha0[k] - k1 * alpha0[n + 1 - k]);
			alpha1[n + 1] = (float) -k1;
			alpha = alpha1;
		}

		// for n in 0 until p
		// K[n + 1] := d[n] ^ -1 summation(k in 0 until n, alpha[n][k] * r[n + 1 - k])
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
		LinearRegression lr0 = stat.linearRegression(xs0, ys);

		float[] variances = Floats_.toArray(length, i -> {
			double residual = ys[i] - lr0.predict(xs0[i]);
			return (float) (residual * residual);
		});

		// conditional heteroskedasticity
		float[][] xs1 = To.array(length, float[].class, i -> copyPadZeroes(variances, i - p, i));
		LinearRegression lr1 = stat.linearRegression(xs1, variances);

		return Floats_.concat(lr0.coefficients, lr1.coefficients);
	}

	public LinearRegression arima(float[] ys, int p, int d, int q) {
		float[] is = vec.of(ys);
		for (int i = 0; i < d; i++)
			is = ts.differencesOn(i, is);
		return arma0(ys, p, q);
	}

	public LinearRegression arma0(float[] ys, int p, int q) {
		int length = ys.length;
		float[] residuals = new float[q];
		LinearRegression lr = null;

		for (int iter = 0; iter < q; iter++) {
			float yiter = ys[iter];
			int iterm1 = iter - 1;
			int iterp1 = iter + 1;
			int ix = Math.min(p, iterm1);
			int jx = Math.min(q, iterm1);

			for (int j = 0; j < ix; j++)
				yiter -= lr.coefficients[j] * ys[iterm1 - j];
			for (int j = 0; j < jx; j++)
				yiter -= lr.coefficients[p + j] * residuals[iterm1 - j];

			residuals[iter] = yiter;

			float[][] xs = To.array(length, float[].class, i -> {
				int p0 = -Math.max(0, i - p);
				int nr = Math.min(iterp1, q);

				float[] fs1 = new float[p + iterp1];
				Arrays.fill(fs1, 0, p0, 0f);
				Floats_.copy(ys, 0, fs1, p0, p - p0);
				Floats_.copy(residuals, 0, fs1, p, nr);
				return fs1;
			});

			lr = stat.linearRegression(xs, ys);
		}

		return lr;
	}

	public float em(float[] xs, int p, int d, int q) { // ARIMA
		for (int i = 0; i < d; i++)
			xs = ts.dropDiff(1, xs);

		float[] xs1 = Floats_.concat(xs, new float[] { em(xs, p, q), });
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
	private float em(float[] xs, int p, int q) { // ARMA
		int maxpq = Math.max(p, q);
		int xLength = xs.length;
		int tsLength = xLength - maxpq;
		float[] xs_ = xs;
		float[] ars = new float[p];
		float[] mas = new float[q];
		float[] eps = new float[q + 1];

		for (int iter = 0; iter < 9; iter++) {

			// xs[t] - eps[t]
			// = ars[0] * xs[t - 1] + ... + ars[p - 1] * xs[t - p]
			// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
			{
				float[][] lrxss = new float[tsLength][p + q];
				float[] lrys = new float[tsLength];

				for (int i = 0; i < tsLength; i++) {
					float[] lrxs = lrxss[i];
					int t = i + maxpq;
					int k = 0;
					for (int j = 1; j <= p; j++)
						lrxs[k++] = xs[t - j];
					for (int j = 1; j <= q; j++)
						lrxs[k++] = eps[t - j];
					lrys[i] = xs[t] - eps[t];
				}

				float[] coeffs = stat.linearRegression(lrxss, lrys).coefficients;
				ars = Arrays.copyOfRange(coeffs, 0, p);
				mas = Arrays.copyOfRange(coeffs, p, p + q);
			}

			// xs[t] - eps[t]
			// - ars[0] * xs[t - 1] - ... - ars[p - 1] * xs[t - p]
			// = eps[t - 1] * mas[0] + ... + eps[t - q] * mas[q - 1]
			{
				float[][] lrxss = new float[tsLength][xLength];
				float[] lrys = new float[tsLength];

				for (int i = 0; i < tsLength; i++) {
					float[] lrxs = lrxss[i];
					float[] ars_ = ars;
					int t = i + maxpq;
					int t_ = t;
					for (int j = 0; j < q; j++)
						lrxs[--t_] = mas[j];
					lrxss[i] = mas;
					lrys[i] = (float) (xs[t] - eps[t] - Ints_.range(p).collectAsDouble(Int_Dbl.sum(j -> ars_[j] * xs_[t - j - 1])));

					eps = stat.linearRegression(lrxss, lrys).coefficients;
				}
			}
		}

		float[] ars_ = ars;
		float[] mas_ = mas;
		float[] eps_ = eps;

		// x[t]
		// = ars[0] * x[t - 1] + ... + ars[p - 1] * x[t - p]
		// + eps[t]
		// + mas[0] * eps[t - 1] + ... + mas[q - 1] * eps[t - q]
		// when t = xLength
		return (float) (0f //
				+ Ints_.range(p).collectAsDouble(Int_Dbl.sum(j -> ars_[j] * xs_[xLength - j - 1])) //
				+ Ints_.range(q).collectAsDouble(Int_Dbl.sum(j -> mas_[j] * eps_[xLength - j - 1])));
	}

	// Digital Processing of Random Signals, Boaz Porat, page 190
	// q << l << fs.length
	public float[] maDurbin(float[] ys, int q, int l) {
		float[] ar = arLevinsonDurbin(ys, l);
		return arLevinsonDurbin(ar, q);
	}

	private float[] copyPadZeroes(float[] fs0, int from, int to) {
		float[] fs1 = new float[to - from];
		int p = -Math.max(0, from);
		Arrays.fill(fs1, 0, p, 0f);
		Floats_.copy(fs0, 0, fs1, p, to - p);
		return fs1;
	}

}
