package suite.math;

import org.junit.Test;

public class DiscreteCosineTransformTest {

	private DiscreteCosineTransform dct = new DiscreteCosineTransform();

	@Test
	public void testDct() {
		float[] fs0 = { 0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, };
		float[] fs1 = dct.dct(fs0);
		float[] fs2 = dct.idct(fs1);
		for (int i = 0; i < fs0.length; i++)
			MathUtil.verifyEquals(fs0[i], fs2[i]);
	}

}
