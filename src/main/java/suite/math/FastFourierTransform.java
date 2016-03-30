package suite.math;

// https://rosettacode.org/wiki/Fast_Fourier_transform#Java
public class FastFourierTransform {

	public Complex[] fft(Complex tds[]) {
		int size = tds.length;
		Complex fds[] = new Complex[size];
		int s = size;
		int bits = 0;

		while (s > 1) {
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

	public static int reverseBits(int bits, int n0) {
		int n1 = 0;
		while (bits-- > 0) {
			n1 = n1 << 1 | n0 & 1;
			n0 >>= 1;
		}
		return n1;
	}

}
