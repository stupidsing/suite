package suite.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MatrixTest {

	@Test
	public void testInverse() {
		Matrix id = Matrix.identity(3);
		assertEquals(id, Matrix.inverse(id));
	}

}
