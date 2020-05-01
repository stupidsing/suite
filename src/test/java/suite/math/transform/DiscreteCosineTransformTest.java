package suite.math.transform;

import org.junit.jupiter.api.Test;
import suite.math.Math_;
import suite.math.linalg.Vector;

public class DiscreteCosineTransformTest {

	private DiscreteCosineTransform dct = new DiscreteCosineTransform();
	private Vector vec = new Vector();

	@Test
	public void testDct0() {
		test(vec.of(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f));
	}

	@Test
	public void testDct1() {
		test(vec.of(0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f));
	}

	private void test(float[] fs0) {
		var fs1 = dct.dct(fs0);
		var fs2 = dct.idct(fs1);
		for (var i = 0; i < fs0.length; i++)
			Math_.verifyEquals(fs0[i], fs2[i]);
	}

}
