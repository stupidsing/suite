package suite.math;

// https://rosettacode.org/wiki/Fast_Fourier_transform#Java
public class FastFourierTransform {

	public Complex[] fft(Complex tds[]) {
		int size = tds.length;
		Complex fds[] = new Complex[size];
		int s = size;
		int bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (int i = 0; i < size; i++)
			fds[reverseBits(bits, i)] = tds[i];

		for (int g = 2; g <= size; g <<= 1) {
			Complex cis[] = new Complex[g];

			for (int i = 0; i < g; i++) {
				double angle = 2 * Math.PI * i / g;
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

	public float[] fft(float tds[]) {
		int size = tds.length / 2;
		float fds[] = new float[size * 2];
		int s = size;
		int bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (int i = 0; i < size; i++) {
			fds[reverseBits(bits, i) * 2 + 0] = tds[i * 2 + 0];
			fds[reverseBits(bits, i) * 2 + 1] = tds[i * 2 + 1];
		}

		for (int g = 2; g <= size; g <<= 1) {
			float cis[] = new float[g * 2];

			for (int i = 0; i < g; i++) {
				double angle = 2 * Math.PI * i / g;
				cis[i * 2 + 0] = (float) Math.cos(angle);
				cis[i * 2 + 1] = (float) -Math.sin(angle);
			}

			int step = g / 2;

			for (int i = 0; i < size; i += g)
				for (int k = 0; k < step; k++) {
					int ie = (i + k) * 2;
					int io = (i + k + step) * 2;

					float ceReal = fds[ie + 0];
					float ceImag = fds[ie + 1];
					float coReal = fds[io + 0];
					float coImag = fds[io + 1];
					float cisReal = cis[k * 2 + 0];
					float cisImag = cis[k * 2 + 1];
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
