package suite.algo;

import suite.adt.IntObjMap;
import suite.adt.IntObjPair;
import suite.math.Cholesky;
import suite.math.MathUtil;
import suite.math.Matrix;
import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.util.To;

public class Statistic {

	private Matrix mtx = new Matrix();

	public float correlation(float[] xs, float[] ys) {
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
		return (float) ((length * sumxy - sumx * sumy)
				/ Math.sqrt((length * sumx2 - sumx * sumx) * (length * sumy2 - sumy * sumy)));
	}

	public float covariance(float[] xs, float[] ys) {
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
		return (float) ((sumxy - sumx * sumy * il) * il);
	}

	// ordinary least squares
	public float[] linearRegression(float[][] x, float[] y) {
		return new Regression(x, y).estimates;
	}

	public class Regression {
		public final float[] estimates;
		public final double r2;
		public final double standardError;

		private Regression(float[][] x, float[] y) {
			int n = y.length;
			float[][] xt = mtx.transpose(x);
			float[][] xtx = mtx.mul(xt, x);
			float[] lr = new Cholesky().inverseMul(xtx).apply(mtx.mul(xt, y));
			float[] estimatedy = To.floatArray(n, i -> mtx.dot(lr, x[i]));
			float meany = mean(y);

			double sst = 0f; // total sum of squares
			double sse = 0f; // estimated sum of squares
			for (int i = 0; i < n; i++) {
				float d0 = y[i] - meany;
				float d1 = estimatedy[i] - meany;
				sst += d0 * d0;
				sse += d1 * d1;
			}

			double ssr = sst - sse; // sum of squared residuals

			estimates = lr;
			r2 = ssr / sst; // 0 -> not accurate, 1 -> totally accurate
			standardError = Math.sqrt(ssr / (n - 2));
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

	public float mean(float[] fs) {
		return (float) mean_(fs);
	}

	public float standardDeviation(float[] fs) {
		return (float) Math.sqrt(var(fs));
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
					+ ", mean = " + MathUtil.format(mean) //
					+ ", range = " + MathUtil.format(min) + "~" + MathUtil.format(max) //
					+ ", sd = " + MathUtil.format(Math.sqrt(var)) //
					+ ")";
		} else
			return "size = 0";
	}

	public float variance(float[] fs) {
		return (float) var(fs);
	}

	private double var(float[] fs) {
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
