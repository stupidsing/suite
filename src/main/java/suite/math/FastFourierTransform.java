package suite.math;

import suite.util.To;

// https://rosettacode.org/wiki/Fast_Fourier_transform#Java
public class FastFourierTransform {

	public Complex[] ifft(Complex[] cs0) {
		int size = cs0.length;
		Complex[] cs1 = To.array(Complex.class, size, i -> cs0[i].conjugate());
		Complex[] cs2 = fft(cs1);
		float inv = 1.0f / size;

		for (int i = 0; i < size; i++)
			cs2[i] = cs2[i].conjugate().scale(inv);

		return cs2;
	}

	public Complex[] fft(Complex[] tds) {
		int size = tds.length;
		Complex[] fds = new Complex[size];
		int s = size;
		int bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (int i = 0; i < size; i++)
			fds[reverseBits(bits, i)] = tds[i];

		for (int g = 2; g <= size; g <<= 1) {
			double angleDiff = 2 * Math.PI / g;
			Complex[] cis = new Complex[g];

			for (int i = 0; i < g; i++) {
				double angle = angleDiff * i;
				cis[i] = Complex.of((float) Math.cos(angle), (float) -Math.sin(angle));
			}

			int step = g / 2;

			for (int i = 0; i < size; i += g)
				for (int k = 0; k < step; k++) {
					int ie = i + k;
					int io = i + k + step;

					Complex ce = fds[ie];
					Complex co = fds[io];
					Complex exp = Complex.mul(cis[k], co);

					fds[ie] = Complex.add(ce, exp);
					fds[io] = Complex.sub(ce, exp);
				}
		}

		return fds;
	}

	public float[] ifft(float[] fs0) {
		int size2 = fs0.length;
		int size = size2 / 2;
		float[] fs1 = new float[size2];

		for (int i2 = 0; i2 < size2; i2 += 2) {
			fs1[i2 + 0] = fs0[i2 + 0];
			fs1[i2 + 1] = -fs0[i2 + 1];
		}

		float[] fs2 = fft(fs1);
		float inv = 1.0f / size;

		for (int i2 = 0; i2 < size2; i2 += 2) {
			fs2[i2 + 0] = fs2[i2 + 0] * inv;
			fs2[i2 + 1] = fs2[i2 + 1] * -inv;
		}

		return fs2;
	}

	public float[] fft(float[] tds) {
		int size2 = tds.length;
		int size = size2 / 2;
		float[] fds = new float[size2];
		int s = size;
		int bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (int i = 0; i < size; i++) {
			int i2 = i * 2;
			fds[reverseBits(bits, i) * 2 + 0] = tds[i2 + 0];
			fds[reverseBits(bits, i) * 2 + 1] = tds[i2 + 1];
		}

		for (int g = 2; g <= size; g <<= 1) {
			int g2 = g * 2;
			float[] cis = new float[g2];
			double angleDiff = Math.PI / g;

			for (int i2 = 0; i2 < g2; i2 += 2) {
				double angle = angleDiff * i2;
				cis[i2 + 0] = (float) Math.cos(angle);
				cis[i2 + 1] = (float) -Math.sin(angle);
			}

			for (int i2 = 0; i2 < size2; i2 += g2)
				for (int k2 = 0; k2 < g; k2 += 2) {
					int ie = i2 + k2;
					int io = ie + g;

					float ceReal = fds[ie + 0];
					float ceImag = fds[ie + 1];
					float coReal = fds[io + 0];
					float coImag = fds[io + 1];
					float cisReal = cis[k2 + 0];
					float cisImag = cis[k2 + 1];
					float expReal = cisReal * coReal - cisImag * coImag;
					float expImag = cisReal * coImag + cisImag * coReal;

					fds[ie + 0] = ceReal + expReal;
					fds[ie + 1] = ceImag + expImag;
					fds[io + 0] = ceReal - expReal;
					fds[io + 1] = ceImag - expImag;
				}
		}

		return fds;
	}

	private static int reverseBits(int bits, int n0) {
		int n1 = 0;
		while (0 < bits--) {
			n1 = n1 << 1 | n0 & 1;
			n0 >>= 1;
		}
		return n1;
	}

}
