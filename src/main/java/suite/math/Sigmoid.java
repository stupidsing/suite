package suite.math;

public class Sigmoid {

	public static float sigmoid(float value) {
		return (float) (1d / (1d + Math.exp(-value)));
	}

	public static float sigmoidGradient(float value) {
		return value * (1f - value);
	}

}
