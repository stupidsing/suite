package suite.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MatrixTest {

	@Test
	public void testInverse() {
		Matrix id = Matrix.identity(3);
		assertEquals(id, Matrix.inverse(id));

		Matrix mul8 = Matrix.mul(id, 8f);
		Matrix div8 = Matrix.mul(id, 1f / 8f);
		assertEquals(mul8, Matrix.inverse(div8));
		assertEquals(div8, Matrix.inverse(mul8));

		Matrix o = new Matrix(2, 2);
		o.v[0][1] = 1;
		o.v[1][0] = 1;
		assertEquals(o, Matrix.inverse(o));
	}

}
