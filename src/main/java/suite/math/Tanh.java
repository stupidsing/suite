package suite.math;

public class Tanh {

	public static float tanh(float value) {
		return (float) Math.tanh(value);
	}

	public static float tanhGradient(float value) {
		return 1f - value * value;
	}

}
