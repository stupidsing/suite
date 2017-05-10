package suite.algo;

import suite.adt.IntObjMap;
import suite.adt.IntObjPair;
import suite.math.Cholesky;
import suite.math.Matrix;
import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.util.To;

public class Statistic {

	private Matrix mtx = new Matrix();

	public final double riskFreeInterestRate = .04d;

	public double correlation(float[] xs, float[] ys) {
		int length = xs.length;
		double sumx = 0d, sumy = 0d;
		double sumx2 = 0d, sumy2 = 0d;
		double sumxy = 0d;
		if (length == ys.length)
			for (int i = 0; i < length; i++) {
				double x = xs[i], y = ys[i];
				sumx += x;
				sumy += y;
				sumx2 += x * x;
				sumy2 += y * y;
				sumxy += x * y;
			}
		else
			throw new RuntimeException("Wrong input sizes");
		return (length * sumxy - sumx * sumy) / Math.sqrt((length * sumx2 - sumx * sumx) * (length * sumy2 - sumy * sumy));
	}

	public double covariance(float[] xs, float[] ys) {
		int length = xs.length;
		double sumx = 0d, sumy = 0d;
		double sumxy = 0d;
		if (length == ys.length)
			for (int i = 0; i < length; i++) {
				double x = xs[i], y = ys[i];
				sumx += x;
				sumy += y;
				sumxy += x * y;
			}
		else
			throw new RuntimeException("Wrong input sizes");
		double il = 1d / length;
		return (sumxy - sumx * sumy * il) * il;
	}

	// ordinary least squares
	public LinearRegression linearRegression(float[][] x, float[] y) {
		return new LinearRegression(x, y);
	}

	public class LinearRegression {
		public final float[] betas;
		public final double r2;
		public final double standardError;

		private LinearRegression(float[][] x, float[] y) {
			int n = y.length;
			float[][] xt = mtx.transpose(x);
			float[][] xtx = mtx.mul(xt, x);
			float[] lr = new Cholesky().inverseMul(xtx).apply(mtx.mul(xt, y));
			betas = lr;

			float[] estimatedy = To.arrayOfFloats(n, i -> predict(x[i]));
			double meany = mean(y);

			double sst = 0f; // total sum of squares
			double ssr = 0f; // estimated sum of squares
			for (int i = 0; i < n; i++) {
				double d0 = y[i] - meany;
				double d1 = estimatedy[i] - meany;
				sst += d0 * d0;
				ssr += d1 * d1;
			}

			// double sse = sst - ssr; // sum of squared residuals

			r2 = ssr / sst; // 0 -> not accurate, 1 -> totally accurate
			standardError = Math.sqrt(ssr / (n - mtx.width(x) - 1));
		}

		public float predict(float[] x) {
			return mtx.dot(betas, x);
		}
	}

	public Obj_Int<int[]> naiveBayes(int[][] x, int[] y) {
		IntObjMap<int[]> xcounts = new IntObjMap<>();
		IntObjMap<int[]> ycounts = new IntObjMap<>();
		int ix = x.length; // number of samples
		int jx = x[0].length; // number of features

		for (int i = 0; i < ix; i++) {
			ycounts.computeIfAbsent(y[i], y_ -> new int[] { 0, })[0]++;
			for (int j = 0; j < jx; j++)
				xcounts.computeIfAbsent(x[i][j], x_ -> new int[] { 0, })[0]++;
		}

		return ins -> {
			IntObjPair<int[]> pair = IntObjPair.of(0, null);
			IntObjSource<int[]> source2 = ycounts.source();
			int result = 0;
			double maxp = Double.MIN_VALUE;

			while (source2.source2(pair)) {
				double p = ((double) pair.t1[0]) / ix;

				for (int j = 0; j < jx; j++)
					p *= xcounts.computeIfAbsent(ins[j], x_ -> new int[] { 0, })[0] / (double) jx;

				if (maxp < p) {
					result = pair.t0;
					maxp = p;
				}
			}

			return result;
		};
	}

	public double skewness(float[] fs) {
		double mean = mean_(fs);
		double sd = standardDeviation(fs);
		double sum = 0d;
		for (float f : fs) {
			double d = f - mean;
			sum += d * d * d;
		}
		double length = fs.length;
		double length1 = length - 1;
		double adjustment = Math.sqrt(length * length1) / length1;
		return adjustment * sum / (length * sd * sd * sd);
	}

	public double kurtosis(float[] fs) {
		double mean = mean_(fs);
		double sd = standardDeviation(fs);
		double sum = 0d;
		for (float f : fs) {
			double d = f - mean;
			double d2 = d * d;
			sum += d2 * d2;
		}
		double sd2 = sd * sd;
		return sum / (fs.length * sd2 * sd2);
	}

	public double mean(float[] fs) {
		return mean_(fs);
	}

	public double standardDeviation(float[] fs) {
		return Math.sqrt(variance(fs));
	}

	public String stats(float[] fs) {
		int length = fs.length;
		if (0 < length) {
			float first = fs[0];
			double min = first, max = first;
			double sum = first, sumsq = first * first;

			for (int i = 1; i < length; i++) {
				float f = fs[i];
				min = Double.min(min, f);
				max = Double.max(max, f);
				sum += f;
				sumsq += f * f;
			}

			double il = 1d / length;
			double mean = sum * il;
			double var = sumsq * il - mean * mean;

			return "(size = " + length //
					+ ", mean = " + To.string(mean) //
					+ ", range = " + To.string(min) + "~" + To.string(max) //
					+ ", sd = " + To.string(Math.sqrt(var)) //
					+ ")";
		} else
			return "size = 0";
	}

	public double variance(float[] fs) {
		int length = fs.length;
		double sum = 0d;
		double sumsq = 0d;

		for (float f : fs) {
			sum += f;
			sumsq += f * f;
		}

		double il = 1d / length;
		double mean = sum * il;
		return sumsq * il - mean * mean;
	}

	private double mean_(float[] fs) {
		int length = fs.length;
		double sum = 0f;
		for (float f : fs)
			sum += f;
		return sum / length;
	}

}
