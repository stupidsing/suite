package suite.math;

// https://rosettacode.org/wiki/Fast_Fourier_transform#Java
public class FastFourierTransform {

	public void fft(Complex buffer[]) {
		int size = buffer.length;
		int s = size;
		int bits = 0;

		while (s > 0) {
			s >>= 1;
			bits++;
		}

		for (int i = 1; i < size / 2; i++) {
			int j = reverseBits(bits, i);
			Complex temp = buffer[i];
			buffer[i] = buffer[j];
			buffer[j] = temp;
		}

		for (int g = 2; g <= size; g <<= 1) {
			Complex cis[] = new Complex[g];

			for (int i = 0; i < g; i++) {
				double angle = 2 * Math.PI * i / g;
				cis[i] = new Complex((float) Math.cos(angle), (float) -Math.sin(angle));
			}

			int step = g / 2;

			for (int i = 0; i < size; i += g)
				for (int k = 0; k < step; k++) {
					int ie = i + k;
					int io = i + k + step;

					Complex ce = buffer[ie];
					Complex co = buffer[io];
					Complex exp = Complex.mul(cis[k], co);

					buffer[ie] = Complex.add(ce, exp);
					buffer[io] = Complex.sub(ce, exp);
				}
		}
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
