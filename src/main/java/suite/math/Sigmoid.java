package suite.math;

public class Sigmoid {

	public static float sigmoid(float value) {
		return 1f / (1f + (float) Math.exp(-value));
	}

	public static float sigmoidGradient(float value) {
		return value * (1f - value);
	}

}
