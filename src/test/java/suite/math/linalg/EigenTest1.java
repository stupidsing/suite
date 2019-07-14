package suite.math.linalg;

import org.junit.Test;

import suite.math.Math_;
import suite.util.To;

public class EigenTest1 {

	private Eigen eigen = new Eigen();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	@Test
	public void test2() {
		test(new float[][] { //
				{ 4f, 3f, }, //
				{ -2f, -3f, }, });
	}

	@Test
	public void test3() {
		test(new float[][] { //
				{ -4f, 14f, 0f, }, //
				{ -5f, 13f, 0f, }, //
				{ -1f, 0f, 2f, }, });
	}

//	@Test
//	public void testDegenerate() {
//		test(new float[][] { //
//				{ 3f, 2f, 6f, }, //
//				{ 2f, 2f, 5f, }, //
//				{ -2f, -1f, -4f, }, });
//	}

	@Test
	public void testPca() {
		var size = 9;
		var m = To.matrix(size, size, (i, j) -> i);
		var pc = eigen.pca(m);
		for (var f : pc)
			Math_.verifyEquals(f, pc[0]);
	}

	private void test(float[][] m) {
		var eigenVectors = eigen.power1(m);

		for (var pair : eigenVectors) {
			var eigenValue = pair.t0;
			var eigenVector = pair.t1;
			String s = "eigen pair = " + To.string(eigenValue) + " :: " + mtx.toString(eigenVector);
			var n0 = norm(eigenVector);
			var n1 = norm(mtx.mul(m, eigenVector));
			vec.verifyEquals(n0, n1, .01f);
			System.out.println(s);
		}
	}

	private float[] norm(float[] v0) {
		var v1 = vec.normalize(v0);
		if (v1[0] < 0f)
			return vec.scale(v1, -1d);
		else
			return v1;
	}

}
