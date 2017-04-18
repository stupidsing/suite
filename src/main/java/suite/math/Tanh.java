package suite.math;

public class Tanh {

	public static float[] tanhOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = tanh(fs[i]);
		return fs;
	}

	public static float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = tanhGradient(fs[i]);
		return fs;
	}

	public static float tanh(float value) {
		return (float) Math.tanh(value);
	}

	public static float tanhGradient(float value) {
		return 1f - value * value;
	}

}
