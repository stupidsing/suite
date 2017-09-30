package suite.math.linalg;

import java.util.Arrays;

import org.junit.Test;

public class GaussSeidelTest {

	private GaussSeidel gs = new GaussSeidel();
	private Matrix_ mtx = new Matrix_();

	@Test
	public void test() {
		float[][] a = { //
				{ 10f, -1f, 2f, 0f, }, //
				{ -1f, 11f, -1f, 3f, }, //
				{ 2f, -1f, 10f, -1f, }, //
				{ 0f, 3f, -1f, 8f, }, //
		};

		float[] actual = gs.solve(a, new float[] { 6f, 25f, -11f, 15f, });
		float[] expect = new float[] { 1f, 2f, -1f, 1f, };
		System.out.println(Arrays.toString(actual));
		mtx.verifyEquals(actual, expect);
	}

}
