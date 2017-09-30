package suite.math.linalg;

import org.junit.Test;

public class EigenTest {

	private Eigen eigen = new Eigen();
	private Matrix_ mtx = new Matrix_();

	@Test
	public void test2() {
		test(new float[][] { //
				{ 4f, 3f, }, //
				{ -2f, -3f, } });
	}

	@Test
	public void test3() {
		test(new float[][] { //
				{ 3f, 2f, 6f, }, //
				{ 2f, 2f, 5f, }, //
				{ -2f, -1f, -4f, } });
	}

	private void test(float[][] m) {
		float[][] eigenVectors = eigen.power(m);

		for (float[] eigenVector : eigenVectors) {
			float[] n0 = norm(eigenVector);
			float[] n1 = norm(mtx.mul(m, eigenVector));
			mtx.verifyEquals(n0, n1, .01f);
		}
	}

	private float[] norm(float[] v0) {
		float[] v1 = mtx.normalize(v0);
		if (v1[0] < 0f)
			return mtx.scale(v1, -1d);
		else
			return v1;
	}

}
