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

		assertTrue(Math.abs(fs[0].r - 4) < .1);
		assertTrue(Math.abs(fs[0].i - 0) < .1);
		assertTrue(Math.abs(fs[1].r - 1) < .1);
		assertTrue(Math.abs(fs[1].i - -2.4) < .1);
		assertTrue(Math.abs(fs[2].r - 0) < .1);
		assertTrue(Math.abs(fs[2].i - 0) < .1);
		assertTrue(Math.abs(fs[3].r - 1) < .1);
		assertTrue(Math.abs(fs[3].i - -0.4) < .1);
		assertTrue(Math.abs(fs[4].r - 0) < .1);
		assertTrue(Math.abs(fs[4].i - 0) < .1);
		assertTrue(Math.abs(fs[5].r - 1) < .1);
		assertTrue(Math.abs(fs[5].i - 0.4) < .1);
		assertTrue(Math.abs(fs[6].r - 0) < .1);
		assertTrue(Math.abs(fs[6].i - 0) < .1);
		assertTrue(Math.abs(fs[7].r - 1) < .1);
		assertTrue(Math.abs(fs[7].i - 2.4) < .1);
	}

}
