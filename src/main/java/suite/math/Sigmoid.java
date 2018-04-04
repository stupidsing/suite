package suite.math;

public class Sigmoid {

	public static float[] sigmoidOn(float[] fs) {
		var length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) sigmoid(fs[i]);
		return fs;
	}

	public static float[] sigmoidGradientOn(float[] fs) {
		var length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) sigmoidGradient(fs[i]);
		return fs;
	}

	public static double sigmoid(double value) {
		return (1d / (1d + Math.exp(-value)));
	}

	public static double sigmoidGradient(double value) {
		return value * (1d - value);
	}

}
