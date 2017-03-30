package suite.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiscreteCosineTransformTest {

	@Test
	public void testDct() {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();
		float fs0[] = { 0, 1, 2, 3, 4, 5, 6, 7, };
		float fs1[] = dct.dct(fs0);
		float fs2[] = dct.idct(fs1);
		for (int i = 0; i < fs0.length; i++)
			assertTrue(0.1 < Math.abs(fs0[i] - fs2[i]));
	}

}
