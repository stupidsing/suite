package suite.math.transform;

import suite.math.Complex;
import suite.util.To;

import static java.lang.Math.*;

// https://rosettacode.org/wiki/Fast_Fourier_transform#Java
public class FastFourierTransform {

	public Complex[] ifft(Complex[] cs0) {
		var size = cs0.length;
		var cs1 = To.array(size, Complex.class, i -> cs0[i].conjugate());
		var cs2 = fft(cs1);
		var inv = 1f / size;

		for (var i = 0; i < size; i++)
			cs2[i] = cs2[i].conjugate().scale(inv);

		return cs2;
	}

	public Complex[] fft(Complex[] tds) {
		var size = tds.length;
		var fds = new Complex[size];
		var s = size;
		var bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (var i = 0; i < size; i++)
			fds[reverseBits(bits, i)] = tds[i];

		for (var g = 2; g <= size; g <<= 1) {
			var angleDiff = 2 * PI / g;
			var cis = new Complex[g];

			for (var i = 0; i < g; i++) {
				var angle = angleDiff * i;
				cis[i] = Complex.of((float) cos(angle), (float) -sin(angle));
			}

			var step = g / 2;

			for (var i = 0; i < size; i += g)
				for (var k = 0; k < step; k++) {
					var ie = i + k;
					var io = i + k + step;

					var ce = fds[ie];
					var co = fds[io];
					var exp = Complex.mul(cis[k], co);

					fds[ie] = Complex.add(ce, exp);
					fds[io] = Complex.sub(ce, exp);
				}
		}

		return fds;
	}

	public float[] ifft(float[] fs0) {
		var size2 = fs0.length;
		var size = size2 / 2;
		var fs1 = new float[size2];

		for (var i2 = 0; i2 < size2; i2 += 2) {
			fs1[i2 + 0] = fs0[i2 + 0];
			fs1[i2 + 1] = -fs0[i2 + 1];
		}

		var fs2 = fft(fs1);
		var inv = 1f / size;

		for (var i2 = 0; i2 < size2; i2 += 2) {
			fs2[i2 + 0] = fs2[i2 + 0] * inv;
			fs2[i2 + 1] = fs2[i2 + 1] * -inv;
		}

		return fs2;
	}

	public float[] fft(float[] tds) {
		var size2 = tds.length;
		var size = size2 / 2;
		var fds = new float[size2];
		var s = size;
		var bits = 0;

		while (1 < s) {
			s >>= 1;
			bits++;
		}

		for (var i = 0; i < size; i++) {
			var i2 = i * 2;
			fds[reverseBits(bits, i) * 2 + 0] = tds[i2 + 0];
			fds[reverseBits(bits, i) * 2 + 1] = tds[i2 + 1];
		}

		for (var g = 2; g <= size; g <<= 1) {
			var g2 = g * 2;
			var cis = new float[g2];
			var angleDiff = PI / g;

			for (var i2 = 0; i2 < g2; i2 += 2) {
				var angle = angleDiff * i2;
				cis[i2 + 0] = (float) cos(angle);
				cis[i2 + 1] = (float) -sin(angle);
			}

			for (var i2 = 0; i2 < size2; i2 += g2)
				for (var k2 = 0; k2 < g; k2 += 2) {
					var ie = i2 + k2;
					var io = ie + g;

					var ceReal = fds[ie + 0];
					var ceImag = fds[ie + 1];
					var coReal = fds[io + 0];
					var coImag = fds[io + 1];
					var cisReal = cis[k2 + 0];
					var cisImag = cis[k2 + 1];
					var expReal = cisReal * coReal - cisImag * coImag;
					var expImag = cisReal * coImag + cisImag * coReal;

					fds[ie + 0] = ceReal + expReal;
					fds[ie + 1] = ceImag + expImag;
					fds[io + 0] = ceReal - expReal;
					fds[io + 1] = ceImag - expImag;
				}
		}

		return fds;
	}

	private static int reverseBits(int bits, int n0) {
		var n1 = 0;
		while (0 < bits--) {
			n1 = n1 << 1 | n0 & 1;
			n0 >>= 1;
		}
		return n1;
	}

}
