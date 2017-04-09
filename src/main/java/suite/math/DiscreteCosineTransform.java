package suite.math;

import suite.util.To;

public class DiscreteCosineTransform {

	private FastFourierTransform fft = new FastFourierTransform();

	// http://dsp.stackexchange.com/questions/2807/fast-cosine-transform-via-fft
	public float[] dct(float fs0[]) {
		int size = fs0.length;
		int size21 = size * 2 - 1;
		float fs1[] = new float[size * 8];

		// signal [a, b, c, d] becomes
		// [0, a, 0, b, 0, c, 0, d, 0, d, 0, c, 0, b, 0, a]
		for (int i = 0; i < size; i++) {
			int j = size21 - i;
			float v = fs0[i];
			fs1[i * 4 + 2] = v;
			fs1[j * 4 + 2] = v;
		}

		// take the FFT to get the spectrum
		// [A, B, C, D, 0, -D, -C, -B, -A, -B, -C, -D, 0, D, C, B]
		float fs2[] = fft.fft(fs1);

		// throw away everything but the first [A, B, C, D]
		float fs3[] = To.floatArray(size, i -> fs2[i * 2]);

		// and you are done
		return fs3;
	}

	public float[] idct(float fs3[]) {
		int size = fs3.length;
		int size0 = 0;
		int size4 = size * 4;
		float fs2[] = new float[size * 8];

		for (int i = 0; i < size; i++) {
			int i2 = i * 2;
			float v = fs3[i];
			fs2[size0 + i2] = v;
			fs2[size4 + i2] = -v;
			if (0 < i) {
				int j2 = size4 - i2;
				fs2[size0 + j2] = -v;
				fs2[size4 + j2] = v;
			}
		}

		float fs1[] = fft.ifft(fs2);
		float fs0[] = To.floatArray(size, i -> fs1[i * 4 + 2]);
		return fs0;
	}

}
