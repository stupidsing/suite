package suite.math.stat;

import suite.adt.map.IntObjMap;
import suite.adt.pair.IntObjPair;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.IntPrimitiveSource.IntObjSource;
import suite.primitive.PrimitiveFun.Int_Double;
import suite.primitive.PrimitiveFun.Int_Float;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Statistic {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Matrix mtx = new Matrix();

	public double correlation(float[] xs, float[] ys) {
		Int_Double xf = i -> xs[i];
		Int_Double yf = i -> ys[i];
		if (xs.length == ys.length)
			return correlation(xf, yf, xs.length);
		else
			throw new RuntimeException("wrong input sizes");
	}

	public double correlation(Int_Double xf, Int_Double yf, int length) {
		double sumx = 0d, sumy = 0d;
		double sumx2 = 0d, sumy2 = 0d;
		double sumxy = 0d;
		for (int i = 0; i < length; i++) {
			double x = xf.apply(i), y = yf.apply(i);
			sumx += x;
			sumy += y;
			sumx2 += x * x;
			sumy2 += y * y;
			sumxy += x * y;
		}
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
			throw new RuntimeException("wrong input sizes");
		double il = 1d / length;
		return (sumxy - sumx * sumy * il) * il;
	}

	public double kurtosis(float[] fs) {
		MeanVariance mv = meanVariance_(fs);
		double mean = mv.mean;
		double sd = mv.standardDeviation();
		double sum = 0d;
		for (float f : fs) {
			double d = f - mean;
			double d2 = d * d;
			sum += d2 * d2;
		}
		double sd2 = sd * sd;
		return sum / (fs.length * sd2 * sd2);
	}

	// ordinary least squares
	public LinearRegression linearRegression(float[][] x, float[] y) {
		return new LinearRegression(x, y);
	}

	public class LinearRegression {
		public final int nSamples;
		public final int sampleLength;
		public final float[][] in;
		public final float[] betas;
		public final double invn2;
		public final double sse;
		public final double r2;
		public final double standardError;

		private LinearRegression(float[][] x, float[] y) {
			int nSamples_ = y.length;
			int sampleLength_ = mtx.width(x);
			float[][] xt = mtx.transpose(x);
			float[][] xtx = mtx.mul(xt, x);
			betas = cholesky.inverseMul(xtx).apply(mtx.mul(xt, y));

			float[] estimatedy = To.arrayOfFloats(x, this::predict);
			double meany = mean_(y);

			double sst = 0d; // total sum of squares
			double ssr = 0d; // estimated sum of squares
			double sse_ = 0d; // sum of squared residuals

			for (int i = 0; i < nSamples_; i++) {
				float yi = y[i];
				float estyi = estimatedy[i];
				double d0 = yi - meany;
				double d1 = estyi - meany;
				double d2 = yi - estyi;
				sst += d0 * d0;
				ssr += d1 * d1;
				sse_ += d2 * d2;
			}

			// sse = sst - ssr; // theoretically
			nSamples = nSamples_;
			sampleLength = sampleLength_;
			in = x;
			invn2 = 1d / (nSamples_ - sampleLength_ - 1);
			sse = sse_;
			r2 = ssr / sst; // 0 -> not accurate, 1 -> totally accurate
			standardError = Math.sqrt(ssr * invn2);
		}

		public float predict(float[] x) {
			return mtx.dot(betas, x);
		}

		// if f-statistic < .05d, we conclude R%2 != 0, the test is significant
		public double fStatistic() {
			return (nSamples - sampleLength - 1) * r2 / (1d - r2);
		}

		public float[] tStatistic() {
			return To.arrayOfFloats(sampleLength, i -> {
				MeanVariance mv = new MeanVariance(in.length, j -> in[j][i]);
				double invsd = Math.sqrt(mv.variance / (sse * invn2));
				return (float) (betas[i] * invsd);
			});
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			float[] tStatistic = tStatistic();
			for (int i = 0; i < sampleLength; i++)
				sb.append("\ncoefficient = " + To.string(betas[i]) + ", t-statistic = " + To.string(tStatistic[i]));
			sb.append("\nstandard error = " + To.string(standardError) + ", r2 = " + To.string(r2));
			return sb.toString();
		}
	}

	// iteratively reweighted least squares
	public LogisticRegression logisticRegression(float[][] x, boolean[] bs) {
		return new LogisticRegression(x, bs);
	}

	public class LogisticRegression {
		private float[] w;

		private LogisticRegression(float[][] x, boolean[] bs) {
			int nSamples = mtx.height(x);
			int sampleLength = mtx.width(x);
			w = new float[sampleLength];

			if (nSamples == bs.length) {
				float[][] xt = mtx.transpose(x);
				float[] y = To.arrayOfFloats(nSamples, i -> bs[i] ? 1f : 0f);

				for (int n = 0; n < 256; n++) {
					float[] bernoulli = To.arrayOfFloats(x, this::predict);
					float[] s = To.arrayOfFloats(bernoulli, b -> b * (1f - b));
					float[][] sx = mtx.of(x);
					for (int i = 0; i < nSamples; i++)
						for (int j = 0; j < sampleLength; j++)
							sx[i][j] *= s[i];

					Fun<float[], float[]> cd = cholesky.inverseMul(mtx.mul_mTn(sx, x));
					w = cd.apply(mtx.mul(xt, mtx.sub(mtx.add(mtx.mul(sx, w), y), bernoulli)));
				}
			} else
				throw new RuntimeException("wrong input sizes");
		}

		public float predict(float[] x) {
			return (float) (1d / (1d + Math.exp(-mtx.dot(w, x))));
		}
	}

	public MeanVariance meanVariance(float[] fs) {
		return meanVariance_(fs);
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
		MeanVariance mv = meanVariance_(fs);
		double mean = mv.mean;
		double sd = mv.standardDeviation();
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
		return meanVariance_(fs).variance;
	}

	public class MeanVariance {
		public final int size;
		public final double min, max;
		public final double sum, sumsq;
		public final double mean, variance;

		private MeanVariance(int length, Int_Float fun) {
			if (0 < length) {
				float first = fun.apply(0);
				double min_ = first, max_ = first;
				double sum_ = first, sumsq_ = first * first;

				for (int i = 1; i < length; i++) {
					float f = fun.apply(i);
					min_ = Double.min(min_, f);
					max_ = Double.max(max_, f);
					sum_ += f;
					sumsq_ += f * f;
				}

				double il = 1d / length;
				double mean_ = sum_ * il;
				size = length;
				min = min_;
				max = max_;
				sum = sum_;
				sumsq = sumsq_;
				mean = mean_;
				variance = sumsq_ * il - mean_ * mean_;
			} else {
				size = 0;
				min = max = sum = sumsq = mean = variance = 0d;
			}

		}

		public double standardDeviation() {
			return Math.sqrt(variance);
		}

		public String toString() {
			return "(size = " + size //
					+ ", mean = " + To.string(mean) //
					+ ", range = " + To.string(min) + "~" + To.string(max) //
					+ ", sd = " + To.string(standardDeviation()) //
					+ ")";
		}
	}

	private double mean_(float[] fs) {
		int length = fs.length;
		double sum = 0f;
		for (float f : fs)
			sum += f;
		return sum / length;
	}

	private MeanVariance meanVariance_(float[] fs) {
		return new MeanVariance(fs.length, i -> fs[i]);
	}

}
