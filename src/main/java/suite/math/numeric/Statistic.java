package suite.math.numeric;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static primal.statics.Fail.fail;

import java.util.List;

import primal.Verbs.Build;
import primal.fp.Funs.Fun;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.Int_Dbl;
import primal.primitive.Int_Flt;
import primal.primitive.adt.IntMutable;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.adt.pair.IntObjPair;
import primal.streamlet.Streamlet;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.util.To;

public class Statistic {

	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	public double correlation(float[] xs, float[] ys) {
		Int_Dbl xf = i -> xs[i];
		Int_Dbl yf = i -> ys[i];
		vec.sameLength(xs, ys);
		return correlation(xf, yf, xs.length);
	}

	public double correlation(Int_Dbl xf, Int_Dbl yf, int length) {
		var sumx = 0d;
		var sumy = 0d;
		var sumx2 = 0d;
		var sumy2 = 0d;
		var sumxy = 0d;
		for (var i = 0; i < length; i++) {
			double x = xf.apply(i), y = yf.apply(i);
			sumx += x;
			sumy += y;
			sumx2 += x * x;
			sumy2 += y * y;
			sumxy += x * y;
		}
		return (length * sumxy - sumx * sumy) / sqrt((length * sumx2 - sumx * sumx) * (length * sumy2 - sumy * sumy));
	}

	public double covariance(float[] xs, float[] ys) {
		var length = vec.sameLength(xs, ys);
		var sumx = 0d;
		var sumy = 0d;
		var sumxy = 0d;
		for (var i = 0; i < length; i++) {
			double x = xs[i], y = ys[i];
			sumx += x;
			sumy += y;
			sumxy += x * y;
		}
		var il = 1d / length;
		return (sumxy - sumx * sumy * il) * il;
	}

	public double kurtosis(float[] fs) {
		return kurtosis_(meanVariance_(fs), fs);
	}

	// ordinary least squares
	public LinearRegression linearRegression(Streamlet<FltObjPair<float[]>> pairs) {
		List<FltObjPair<float[]>> list = pairs.toList();
		var size = list.size();
		var x = To.array(size, float[].class, i -> list.get(i).v);
		var y = To.vector(size, i -> list.get(i).k);
		return linearRegression(x, y, null);
	}

	public LinearRegression linearRegression(float[][] x, float[] y, String[] coefficientNames) {
		return new LinearRegression(x, y, coefficientNames);
	}

	public class LinearRegression {
		public final int nDataPoints;
		public final int nDepVariables;
		public final float[][] in;
		public final float[] coefficients;
		public final String[] coefficientNames;
		public final float[] residuals;
		public final double invn2;
		public final double sst, sse;
		public final double r2;
		public final double standardError;

		private LinearRegression(float[][] x, float[] y, String[] coefficientNames_) {
			var nDataPoints_ = y.length;
			var nDepVariables_ = mtx.width(x);
			var xt = mtx.transpose(x);
			var coeffs = cholesky.inverseMul(mtx.mul(xt, x)).apply(mtx.mul(xt, y));
			var estimatedy = mtx.mul(x, coeffs);
			var residuals_ = vec.sub(y, estimatedy);
			var meany = mean_(y);
			var sst_ = 0d; // total sum of squares
			var ssr = 0d; // estimated sum of squares
			var sse_ = vec.dot(residuals_); // sum of squared residuals

			for (var i = 0; i < nDataPoints_; i++) {
				var d0 = y[i] - meany;
				var d1 = estimatedy[i] - meany;
				sst_ += d0 * d0;
				ssr += d1 * d1;
			}

			// sse = sst - ssr; // theoretically

			nDataPoints = nDataPoints_;
			nDepVariables = nDepVariables_;
			in = x;
			coefficients = coeffs;
			coefficientNames = coefficientNames_ != null //
					? coefficientNames_ //
					: To.array(nDepVariables_, String.class, i -> "c" + i);
			residuals = residuals_;
			invn2 = 1d / (nDataPoints_ - nDepVariables_ - 1);
			sst = sst_;
			sse = sse_;
			r2 = ssr / sst_; // 0 -> not accurate, 1 -> totally accurate
			standardError = sqrt(ssr * invn2);
		}

		public double predict(float[] x) {
			return vec.dot(coefficients, x);
		}

		public double aic() {
			return 2d * (nDepVariables - logLikelihood());
		}

		public double bic() {
			return log(nDataPoints) * nDepVariables - 2d * logLikelihood();
		}

		public float[] coefficients() {
			return !Double.isNaN(sse) ? coefficients : fail();
		}

		public double logLikelihood() {
			var variance = sst / (nDataPoints - nDepVariables - 1);
			return -.5d * (nDataPoints * (log(2 * Math.PI) + log(variance)) + sse / variance);
		}

		// if f-statistic < .05d, we conclude R%2 != 0, the test is significant
		public double fStatistic() {
			return (nDataPoints - nDepVariables - 1) * r2 / (1d - r2);
		}

		// the t statistic is the coefficient divided by its standard error
		public float[] tStatistic() {
			return To.vector(nDepVariables, i -> {
				var mv = new MeanVariance(in.length, j -> in[j][i]);
				var invsd = sqrt(mv.variance / (sse * invn2));
				return (float) (coefficients[i] * invsd);
			});
		}

		public String toString() {
			return Build.string(sb -> {
				var tStatistic = tStatistic();
				for (var i = 0; i < nDepVariables; i++)
					sb.append("\n" + coefficientNames[i] + " = " + To.string(coefficients[i]) //
							+ ", t-statistic = " + To.string(tStatistic[i]));
				sb.append("\nstandard error = " + To.string(standardError) + ", r2 = " + To.string(r2));
			});
		}
	}

	// iteratively reweighted least squares
	public LogisticRegression logisticRegression(float[][] x, boolean[] bs) {
		return new LogisticRegression(x, bs);
	}

	public class LogisticRegression {
		private float[] w;

		private LogisticRegression(float[][] x, boolean[] bs) {
			var nSamples = mtx.height(x);
			var sampleLength = mtx.width(x);
			w = new float[sampleLength];

			if (nSamples == bs.length) {
				var xt = mtx.transpose(x);
				var y = To.vector(nSamples, i -> bs[i] ? 1f : 0f);

				for (var n = 0; n < 256; n++) {
					var bernoulli = To.vector(x, this::predict);
					var s = To.vector(bernoulli, b -> b * (1f - b));
					var sx = mtx.copyOf(x);
					for (var i = 0; i < nSamples; i++)
						for (var j = 0; j < sampleLength; j++)
							sx[i][j] *= s[i];

					Fun<float[], float[]> cd = cholesky.inverseMul(mtx.mul_mTn(sx, x));
					w = cd.apply(mtx.mul(xt, vec.sub(vec.add(mtx.mul(sx, w), y), bernoulli)));
				}
			} else
				fail("wrong input sizes");
		}

		public float predict(float[] x) {
			return (float) (1d / (1d + exp(-vec.dot(w, x))));
		}
	}

	public double mean(float[] fs) {
		return mean_(fs);
	}

	public MeanVariance meanVariance(float[] fs) {
		return meanVariance_(fs);
	}

	public String moments(float[] fs) {
		var mv = meanVariance_(fs);
		return "mean = " + mv.mean //
				+ ", variance = " + mv.variance //
				+ ", skewness = " + skewness_(mv, fs) //
				+ ", kurtosis = " + kurtosis_(mv, fs);
	}

	public Obj_Int<int[]> naiveBayes0(int[][] x, int[] y) {
		var xcounts = new IntObjMap<IntMutable>();
		var ycounts = new IntObjMap<IntMutable>();
		var ix = x.length; // number of samples
		var jx = x[0].length; // number of features

		for (var i = 0; i < ix; i++) {
			ycounts.computeIfAbsent(y[i], y_ -> IntMutable.of(0)).increment();
			for (var j = 0; j < jx; j++)
				xcounts.computeIfAbsent(x[i][j], x_ -> IntMutable.of(0)).increment();
		}

		return ins -> {
			IntObjPair<IntMutable> pair = IntObjPair.of(0, null);
			var source2 = ycounts.source();
			var result = 0;
			var maxp = Double.MIN_VALUE;

			while (source2.source2(pair)) {
				var p = ((double) pair.v.value()) / ix;

				for (var j = 0; j < jx; j++)
					p *= xcounts.computeIfAbsent(ins[j], x_ -> IntMutable.of(0)).value() / (double) jx;

				if (maxp < p) {
					result = pair.k;
					maxp = p;
				}
			}

			return result;
		};
	}

	public double project(float[] fs0, float[] fs1) {
		return vec.dot(fs1, fs0) / vec.dot(fs0);
	}

	// https://medium.com/@raghavan99o/scatter-matrix-covariance-and-correlation-explained-14921741ca56
	// The scatter matrix contains the relation between each combination of the
	// variables.
	public float[][] scatterMatrix(float[][] samples) {
		var nSamples = samples.length;
		var nParameters = samples[0].length;
		var means = new float[nParameters];

		for (var sample : samples)
			vec.addOn(means, sample);

		vec.scaleOn(means, 1d / nSamples);

		var scatter = new float[nParameters][nParameters];

		for (var sample : samples) {
			var dev = vec.sub(sample, means);
			mtx.addOn(scatter, mtx.mul(dev));
		}

		return scatter;
	}

	public double skewness(float[] fs) {
		return skewness_(meanVariance_(fs), fs);
	}

	public double variance(float[] fs) {
		return meanVariance_(fs).variance;
	}

	private double mean_(float[] fs) {
		var length = fs.length;
		var sum = 0f;
		for (var f : fs)
			sum += f;
		return sum / length;
	}

	private MeanVariance meanVariance_(float[] fs) {
		return new MeanVariance(fs.length, i -> fs[i]);
	}

	private double skewness_(MeanVariance mv, float[] fs) {
		var mean = mv.mean;
		var sd = mv.standardDeviation();
		var sum = 0d;
		for (var f : fs) {
			var d = f - mean;
			sum += d * d * d;
		}
		var length = fs.length;
		var length1 = length - 1;
		var adjustment = sqrt(length * length1) / length1;
		return adjustment * sum / (length * sd * sd * sd);
	}

	private double kurtosis_(MeanVariance mv, float[] fs) {
		var mean = mv.mean;
		var sd = mv.standardDeviation();
		var sum = 0d;
		for (var f : fs) {
			var d = f - mean;
			var d2 = d * d;
			sum += d2 * d2;
		}
		var sd2 = sd * sd;
		return sum / (fs.length * sd2 * sd2);
	}

	public class MeanVariance {
		public final int size;
		public final double min, max;
		public final double sum, sumsq;
		public final double mean, variance;

		private MeanVariance(int length, Int_Flt fun) {
			if (0 < length) {
				var first = fun.apply(0);
				double min_ = first, max_ = first;
				double sum_ = first, sumsq_ = first * first;

				for (var i = 1; i < length; i++) {
					var f = fun.apply(i);
					min_ = Double.min(min_, f);
					max_ = Double.max(max_, f);
					sum_ += f;
					sumsq_ += f * f;
				}

				var il = 1d / length;
				var mean_ = sum_ * il;
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
			return sqrt(variance);
		}

		public double volatility() {
			return standardDeviation() / mean;
		}

		public String toString() {
			return "(mean:" + To.string(mean) //
					+ " sd:" + To.string(standardDeviation()) //
					+ " range:" + To.string(min) + "~" + To.string(max) //
					+ ")";
		}
	}

}
