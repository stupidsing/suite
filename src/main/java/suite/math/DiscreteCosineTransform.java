package suite.math;

public class DiscreteCosineTransform {

	public float[] dct(float fs0[]) {
		int size = fs0.length;
		int size1 = size - 1;
		float fs1[] = new float[size * 4];
		int i = 0;

		for (; i < size; i++) {
			fs1[i * 2 + 0] = fs0[i];
			fs1[i * 2 + 1] = 0;
		}

		for (; i < size * 2; i++) {
			fs1[i * 2 + 0] = fs0[size1 - i];
			fs1[i * 2 + 1] = 0;
		}

		float fs2[] = new FastFourierTransform().fft(fs1);
		float fs3[] = new float[size];

		for (int j = 0; j < size; j++) {
			double angle = Math.PI * j / (2 * size);
			float cisReal = (float) Math.cos(angle);
			float cisImag = -(float) Math.sin(angle);
			float fs2Real = fs2[j * 2 + 0];
			float fs2Imag = fs2[j * 2 + 1];
			fs3[j] = cisReal * fs2Real - cisImag * fs2Imag;
		}

		return fs3;
	}

}
