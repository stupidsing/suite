package suite.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatrixTest {

	@Test
	public void testInverse() {
		float id[][] = Matrix.identity(3);
		assertTrue(Matrix.equals(id, Matrix.inverse(id)));

		float mul8[][] = Matrix.mul(id, 8f);
		float div8[][] = Matrix.mul(id, 1f / 8f);
		assertTrue(Matrix.equals(mul8, Matrix.inverse(div8)));
		assertTrue(Matrix.equals(div8, Matrix.inverse(mul8)));

		float o[][] = new float[2][2];
		o[0][1] = 1;
		o[1][0] = 1;
		assertTrue(Matrix.equals(o, Matrix.inverse(o)));
	}

}
