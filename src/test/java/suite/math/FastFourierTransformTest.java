package suite.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FastFourierTransformTest {

	private FastFourierTransform fft = new FastFourierTransform();

	@Test
	public void testFft() {
		Complex zero = Complex.of(0f, 0f);
		Complex one = Complex.of(1f, 0f);
		Complex fs0[] = { one, one, one, one, zero, zero, zero, zero, };
		Complex fs1[] = fft.fft(fs0);
		Complex fs2[] = fft.ifft(fs1);

		assertEquals(fs1[0], Complex.of(4f, 0f));
		assertEquals(fs1[1], Complex.of(1f, -2.4f));
		assertEquals(fs1[2], Complex.of(0f, 0f));
		assertEquals(fs1[3], Complex.of(1f, -.4f));
		assertEquals(fs1[4], Complex.of(0f, 0f));
		assertEquals(fs1[5], Complex.of(1f, .4f));
		assertEquals(fs1[6], Complex.of(0f, 0f));
		assertEquals(fs1[7], Complex.of(1f, 2.4f));

		for (int i = 0; i < fs0.length; i++)
			assertEquals(fs0[i], fs2[i]);
	}

	@Test
	public void testFftFloat() {
		float fs0[] = { 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, };
		float fs1[] = fft.fft(fs0);
		float fs2[] = fft.ifft(fs1);

		assertEquals(fs1[0], 4f);
		assertEquals(fs1[1], 0f);
		assertEquals(fs1[2], 1f);
		assertEquals(fs1[3], -2.4f);
		assertEquals(fs1[4], 0f);
		assertEquals(fs1[5], 0f);
		assertEquals(fs1[6], 1f);
		assertEquals(fs1[7], -.4f);
		assertEquals(fs1[8], 0f);
		assertEquals(fs1[9], 0f);
		assertEquals(fs1[10], 1f);
		assertEquals(fs1[11], .4f);
		assertEquals(fs1[12], 0f);
		assertEquals(fs1[13], 0f);
		assertEquals(fs1[14], 1f);
		assertEquals(fs1[15], 2.4f);

		for (int i = 0; i < fs0.length; i++)
			assertEquals(fs0[i], fs2[i]);
	}

	private void assertEquals(Complex a, Complex b) {
		assertEquals(a.r, b.r);
		assertEquals(a.i, b.i);
	}

	private void assertEquals(float a, float b) {
		assertTrue(Math.abs(a - b) < .1);
	}

}
