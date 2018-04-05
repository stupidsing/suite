package suite.math.transform;

import org.junit.Test;

import suite.math.Complex;
import suite.math.MathUtil;

public class FastFourierTransformTest {

	private FastFourierTransform fft = new FastFourierTransform();

	@Test
	public void testFft() {
		Complex zero = Complex.of(0f, 0f);
		Complex one = Complex.of(1f, 0f);
		Complex[] fs0 = { one, one, one, one, zero, zero, zero, zero, };
		Complex[] fs1 = fft.fft(fs0);
		Complex[] fs2 = fft.ifft(fs1);

		Complex.verifyEquals(fs1[0], Complex.of(4f, 0f));
		Complex.verifyEquals(fs1[1], Complex.of(1f, -2.414214f));
		Complex.verifyEquals(fs1[2], Complex.of(0f, 0f));
		Complex.verifyEquals(fs1[3], Complex.of(1f, -.414214f));
		Complex.verifyEquals(fs1[4], Complex.of(0f, 0f));
		Complex.verifyEquals(fs1[5], Complex.of(1f, .414214f));
		Complex.verifyEquals(fs1[6], Complex.of(0f, 0f));
		Complex.verifyEquals(fs1[7], Complex.of(1f, 2.414214f));

		for (var i = 0; i < fs0.length; i++)
			Complex.verifyEquals(fs0[i], fs2[i]);
	}

	@Test
	public void testFftFloat() {
		float[] fs0 = { 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, };
		var fs1 = fft.fft(fs0);
		var fs2 = fft.ifft(fs1);

		MathUtil.verifyEquals(fs1[0], 4f);
		MathUtil.verifyEquals(fs1[1], 0f);
		MathUtil.verifyEquals(fs1[2], 1f);
		MathUtil.verifyEquals(fs1[3], -2.414214f);
		MathUtil.verifyEquals(fs1[4], 0f);
		MathUtil.verifyEquals(fs1[5], 0f);
		MathUtil.verifyEquals(fs1[6], 1f);
		MathUtil.verifyEquals(fs1[7], -.414214f);
		MathUtil.verifyEquals(fs1[8], 0f);
		MathUtil.verifyEquals(fs1[9], 0f);
		MathUtil.verifyEquals(fs1[10], 1f);
		MathUtil.verifyEquals(fs1[11], .414214f);
		MathUtil.verifyEquals(fs1[12], 0f);
		MathUtil.verifyEquals(fs1[13], 0f);
		MathUtil.verifyEquals(fs1[14], 1f);
		MathUtil.verifyEquals(fs1[15], 2.414214f);

		for (var i = 0; i < fs0.length; i++)
			MathUtil.verifyEquals(fs0[i], fs2[i]);
	}

}
