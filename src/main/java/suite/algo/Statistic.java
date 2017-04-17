package suite.algo;

public class Statistic {

	public static float correlation(float[] xs, float[] ys) {
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

	public static float covariance(float[] xs, float[] ys) {
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

	public static float mean(float[] fs) {
		double mean = mean_(fs);
		return (float) mean;
	}

	public static float standardDeviation(float[] fs) {
		return (float) Math.sqrt(var(fs));
	}

	public static float variance(float[] fs) {
		return (float) var(fs);
	}

	private static double var(float[] fs) {
		int length = fs.length;
		double mean = mean_(fs);
		double sum = 0f;
		for (int i = 0; i < length; i++) {
			double diff = fs[i] - mean;
			sum += diff * diff;
		}
		double var = sum / length;
		return var;
	}

	private static double mean_(float[] fs) {
		int length = fs.length;
		double sum = 0f;
		for (int i = 0; i < length; i++)
			sum += fs[i];
		return sum / length;
	}

}
