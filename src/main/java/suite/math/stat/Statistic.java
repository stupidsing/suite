package suite.math.stat;

import java.util.List;

import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.primitive.Floats_;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Dbl;
import suite.primitive.Int_Flt;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Statistic {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	public double correlation(float[] xs, float[] ys) {
		Int_Dbl xf = i -> xs[i];
		Int_Dbl yf = i -> ys[i];
		vec.sameLength(xs, ys);
		return correlation(xf, yf, xs.length);
	}

	public double correlation(Int_Dbl xf, Int_Dbl yf, int length) {
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
		int length = vec.sameLength(xs, ys);
		double sumx = 0d, sumy = 0d;
		double sumxy = 0d;
		for (int i = 0; i < length; i++) {
			double x = xs[i], y = ys[i];
			sumx += x;
			sumy += y;
			sumxy += x * y;
		}
		double il = 1d / length;
		return (sumxy - sumx * sumy * il) * il;
	}

	public double kurtosis(float[] fs) {
		return kurtosis_(meanVariance_(fs), fs);
	}

	// ordinary least squares
	public LinearRegression linearRegression(List<FltObjPair<float[]>> pairs) {
		int size = pairs.size();
		float[][] x = To.array(size, float[].class, i -> pairs.get(i).t1);
		float[] y = Floats_.toArray(size, i -> pairs.get(i).t0);
		return linearRegression(x, y, null);
	}

	public LinearRegression linearRegression(float[][] x, float[] y, String[] coefficientNames) {
		return new LinearRegression(x, y, coefficientNames);
	}

	public class LinearRegression {
		public final int nSamples;
		public final int sampleLength;
		public final float[][] in;
		public final float[] coefficients;
		public final String[] coefficientNames;
		public final double invn2;
		public final double sst, sse;
		public final double r2;
		public final double standardError;

		private LinearRegression(float[][] x, float[] y, String[] coefficientNames_) {
			int nSamples_ = y.length;
			int sampleLength_ = mtx.width(x);
			float[][] xt = mtx.transpose(x);
			float[][] xtx = mtx.mul(xt, x);
			coefficients = cholesky.inverseMul(xtx).apply(mtx.mul(xt, y));
			coefficientNames = coefficientNames_ != null //
					? coefficientNames_ //
					: To.array(sampleLength_, String.class, i -> "c" + i);

			float[] estimatedy = To.arrayOfFloats(x, this::predict);
			double meany = mean_(y);

			double sst_ = 0d; // total sum of squares
			double ssr = 0d; // estimated sum of squares
			double sse_ = 0d; // sum of squared residuals

			for (int i = 0; i < nSamples_; i++) {
				float yi = y[i];
				float estyi = estimatedy[i];
				double d0 = yi - meany;
				double d1 = estyi - meany;
				double d2 = yi - estyi;
				sst_ += d0 * d0;
				ssr += d1 * d1;
				sse_ += d2 * d2;
			}

			// sse = sst - ssr; // theoretically
			nSamples = nSamples_;
			sampleLength = sampleLength_;
			in = x;
			invn2 = 1d / (nSamples_ - sampleLength_ - 1);
			sst = sst_;
			sse = sse_;
			r2 = ssr / sst_; // 0 -> not accurate, 1 -> totally accurate
			standardError = Math.sqrt(ssr * invn2);
		}

		public float predict(float[] x) {
			return vec.dot(coefficients, x);
		}

		public double aic() {
			return 2d * (sampleLength - logLikelihood());
		}

		public double bic() {
			return Math.log(nSamples) * sampleLength - 2d * logLikelihood();
		}

		public double logLikelihood() {
			double variance = sst / (nSamples - sampleLength - 1);
			return -.5d * (nSamples * (Math.log(2 * Math.PI) + Math.log(variance)) + sse / variance);
		}

		// if f-statistic < .05d, we conclude R%2 != 0, the test is significant
		public double fStatistic() {
			return (nSamples - sampleLength - 1) * r2 / (1d - r2);
		}

		// the t statistic is the coefficient divided by its standard error
		public float[] tStatistic() {
			return Floats_.toArray(sampleLength, i -> {
				MeanVariance mv = new MeanVariance(in.length, j -> in[j][i]);
				double invsd = Math.sqrt(mv.variance / (sse * invn2));
				return (float) (coefficients[i] * invsd);
			});
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			float[] tStatistic = tStatistic();
			for (int i = 0; i < sampleLength; i++)
				sb.append("\n" + coefficientNames[i] + " = " + To.string(coefficients[i]) //
						+ ", t-statistic = " + To.string(tStatistic[i]));
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
				float[] y = Floats_.toArray(nSamples, i -> bs[i] ? 1f : 0f);

				for (int n = 0; n < 256; n++) {
					float[] bernoulli = To.arrayOfFloats(x, this::predict);
					float[] s = To.arrayOfFloats(bernoulli, b -> b * (1f - b));
					float[][] sx = mtx.of(x);
					for (int i = 0; i < nSamples; i++)
						for (int j = 0; j < sampleLength; j++)
							sx[i][j] *= s[i];

					Fun<float[], float[]> cd = cholesky.inverseMul(mtx.mul_mTn(sx, x));
					w = cd.apply(mtx.mul(xt, vec.sub(vec.add(mtx.mul(sx, w), y), bernoulli)));
				}
			} else
				throw new RuntimeException("wrong input sizes");
		}

		public float predict(float[] x) {
			return (float) (1d / (1d + Math.exp(-vec.dot(w, x))));
		}
	}

	public double mean(float[] fs) {
		return mean_(fs);
	}

	public MeanVariance meanVariance(float[] fs) {
		return meanVariance_(fs);
	}

	public String moments(float[] fs) {
		MeanVariance mv = meanVariance_(fs);
		return "mean = " + mv.mean //
				+ ", variance = " + mv.variance //
				+ ", skewness = " + skewness_(mv, fs) //
				+ ", kurtosis = " + kurtosis_(mv, fs);
	}

	public Obj_Int<int[]> naiveBayes0(int[][] x, int[] y) {
		IntObjMap<IntMutable> xcounts = new IntObjMap<>();
		IntObjMap<IntMutable> ycounts = new IntObjMap<>();
		int ix = x.length; // number of samples
		int jx = x[0].length; // number of features

		for (int i = 0; i < ix; i++) {
			ycounts.computeIfAbsent(y[i], y_ -> IntMutable.of(0)).increment();
			for (int j = 0; j < jx; j++)
				xcounts.computeIfAbsent(x[i][j], x_ -> IntMutable.of(0)).increment();
		}

		return ins -> {
			IntObjPair<IntMutable> pair = IntObjPair.of(0, null);
			IntObjSource<IntMutable> source2 = ycounts.source();
			int result = 0;
			double maxp = Double.MIN_VALUE;

			while (source2.source2(pair)) {
				double p = ((double) pair.t1.get()) / ix;

				for (int j = 0; j < jx; j++)
					p *= xcounts.computeIfAbsent(ins[j], x_ -> IntMutable.of(0)).get() / (double) jx;

				if (maxp < p) {
					result = pair.t0;
					maxp = p;
				}
			}

			return result;
		};
	}

	public double project(float[] fs0, float[] fs1) {
		return vec.dot(fs1, fs0) / vec.dot(fs0);
	}

	public double skewness(float[] fs) {
		return skewness_(meanVariance_(fs), fs);
	}

	public double variance(float[] fs) {
		return meanVariance_(fs).variance;
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

	private double skewness_(MeanVariance mv, float[] fs) {
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

	private double kurtosis_(MeanVariance mv, float[] fs) {
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

	public class MeanVariance {
		public final int size;
		public final double min, max;
		public final double sum, sumsq;
		public final double mean, variance;

		private MeanVariance(int length, Int_Flt fun) {
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

		public double volatility() {
			return standardDeviation() / mean;
		}

		public String toString() {
			return "(n:" + size //
					+ " mean:" + To.string(mean) //
					+ " range:" + To.string(min) + "~" + To.string(max) //
					+ " sd:" + To.string(standardDeviation()) //
					+ ")";
		}
	}

}
