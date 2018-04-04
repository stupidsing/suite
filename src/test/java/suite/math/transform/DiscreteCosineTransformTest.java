package suite.math.transform;

import org.junit.Test;

import suite.math.MathUtil;

public class DiscreteCosineTransformTest {

	private DiscreteCosineTransform dct = new DiscreteCosineTransform();

	@Test
	public void testDct0() {
		test(new float[] { 0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, });
	}

	@Test
	public void testDct1() {
		test(new float[] { 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, });
	}

	private void test(float[] fs0) {
		var fs1 = dct.dct(fs0);
		var fs2 = dct.idct(fs1);
		for (int i = 0; i < fs0.length; i++)
			MathUtil.verifyEquals(fs0[i], fs2[i]);
	}

}
