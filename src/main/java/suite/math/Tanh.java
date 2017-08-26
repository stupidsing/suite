package suite.math;

public class Tanh {

	public static float[] tanhOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) tanh(fs[i]);
		return fs;
	}

	public static float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) tanhGradient(fs[i]);
		return fs;
	}

	public static double tanh(double value) {
		return Math.tanh(value);
	}

	public static double tanhGradient(double value) {
		return 1d - value * value;
	}

}
