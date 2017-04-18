package suite.math;

public class Sigmoid {

	public static float[] sigmoidOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = sigmoid(fs[i]);
		return fs;
	}

	public static float[] sigmoidGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = sigmoidGradient(fs[i]);
		return fs;
	}

	public static float sigmoid(float value) {
		return (float) (1d / (1d + Math.exp(-value)));
	}

	public static float sigmoidGradient(float value) {
		return value * (1f - value);
	}

}
