package suite.algo;

public class Statistic {

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
