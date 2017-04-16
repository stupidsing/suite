package suite.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatrixTest {

	private static Matrix mtx = new Matrix();

	@Test
	public void testInverse() {
		float[][] id = mtx.identity(3);
		assertTrue(mtx.equals(id, mtx.inverse(id)));

		float[][] mul8 = mtx.scale(id, 8f);
		float[][] div8 = mtx.scale(id, 1f / 8f);
		assertTrue(mtx.equals(mul8, mtx.inverse(div8)));
		assertTrue(mtx.equals(div8, mtx.inverse(mul8)));

		float[][] o = new float[2][2];
		o[0][1] = 1;
		o[1][0] = 1;
		assertTrue(mtx.equals(o, mtx.inverse(o)));
	}

}
