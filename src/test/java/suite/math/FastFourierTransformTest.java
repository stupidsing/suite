package suite.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FastFourierTransformTest {

	@Test
	public void testFft() {
		Complex zero = Complex.of(0, 0);
		Complex one = Complex.of(1, 0);
		Complex fs[] = new FastFourierTransform().fft(new Complex[] { one, one, one, one, zero, zero, zero, zero, });

		for (Complex f : fs)
			System.out.println(f);

		assertTrue(equals(fs[0].r, 4f));
		assertTrue(equals(fs[0].i, 0f));
		assertTrue(equals(fs[1].r, 1f));
		assertTrue(equals(fs[1].i, -2.4f));
		assertTrue(equals(fs[2].r, 0f));
		assertTrue(equals(fs[2].i, 0f));
		assertTrue(equals(fs[3].r, 1f));
		assertTrue(equals(fs[3].i, -0.4f));
		assertTrue(equals(fs[4].r, 0f));
		assertTrue(equals(fs[4].i, 0f));
		assertTrue(equals(fs[5].r, 1f));
		assertTrue(equals(fs[5].i, 0.4f));
		assertTrue(equals(fs[6].r, 0f));
		assertTrue(equals(fs[6].i, 0f));
		assertTrue(equals(fs[7].r, 1f));
		assertTrue(equals(fs[7].i, 2.4f));
	}

	@Test
	public void testFftFloat() {
		float fs[] = new FastFourierTransform().fft(new float[] { 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, });

		for (float f : fs)
			System.out.println(f);

		assertTrue(equals(fs[0], 4f));
		assertTrue(equals(fs[1], 0f));
		assertTrue(equals(fs[2], 1f));
		assertTrue(equals(fs[3], -2.4f));
		assertTrue(equals(fs[4], 0f));
		assertTrue(equals(fs[5], 0f));
		assertTrue(equals(fs[6], 1f));
		assertTrue(equals(fs[7], -0.4f));
		assertTrue(equals(fs[8], 0f));
		assertTrue(equals(fs[9], 0f));
		assertTrue(equals(fs[10], 1f));
		assertTrue(equals(fs[11], 0.4f));
		assertTrue(equals(fs[12], 0f));
		assertTrue(equals(fs[13], 0f));
		assertTrue(equals(fs[14], 1f));
		assertTrue(equals(fs[15], 2.4f));
	}

	private boolean equals(float a, float b) {
		return Math.abs(a - b) < .1;
	}

}
