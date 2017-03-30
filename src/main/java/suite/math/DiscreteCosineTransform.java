package suite.math;

public class DiscreteCosineTransform {

	public float[] dct(float fs0[]) {
		int size = fs0.length;
		float fs1[] = new float[size * 4];
		int i = 0;

		for (; i < size; i++) {
			fs1[i * 2 + 0] = fs0[i];
			fs1[i * 2 + 1] = 0;
		}

		for (; i < size * 2; i++) {
			fs1[i * 2 + 0] = fs0[2 * size - i - 1];
			fs1[i * 2 + 1] = 0;
		}

		float fs2[] = new FastFourierTransform().fft(fs1);
		float fs3[] = new float[size];
		double angleStep = Math.PI / (2 * size);

		for (int j = 0; j < size; j++) {
			double angle = angleStep * j;
			float cisReal = (float) Math.cos(angle);
			float cisImag = -(float) Math.sin(angle);
			float fs2Real = fs2[j * 2 + 0];
			float fs2Imag = fs2[j * 2 + 1];
			fs3[j] = cisReal * fs2Real - cisImag * fs2Imag;
		}

		return fs3;
	}

	public float[] idct(float fs0[]) {
		int size = fs0.length;
		float fs1[] = new float[size * 4];
		double angleStep = Math.PI / (2 * size);

		for (int i = 0; i < size; i++) {
			double angle = angleStep * i;
			float cisReal = (float) Math.cos(angle);
			float cisImag = -(float) Math.sin(angle);
			float iabs2 = 1 / (cisReal * cisReal + cisImag * cisImag);
			float icisReal = cisReal * iabs2;
			float icisImag = -cisImag * iabs2;
			fs1[i * 2 + 0] = icisReal * fs0[i];
			fs1[i * 2 + 1] = icisImag * fs0[i];
		}

		float fs2[] = new FastFourierTransform().ifft(fs1);
		float fs3[] = new float[size];

		for (int i = 0; i < size; i++)
			fs3[i] = fs2[i * 2];

		return fs3;
	}

}
