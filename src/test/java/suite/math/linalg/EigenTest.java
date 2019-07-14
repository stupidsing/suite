package suite.math.linalg;

import java.util.List;

import org.junit.Test;

import suite.math.Math_;
import suite.primitive.adt.pair.DblObjPair;
import suite.util.To;

public class EigenTest {

	private Eigen eigen = new Eigen();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	@Test
	public void test2() {
		var m = new float[][] { //
				{ 4f, 3f, }, //
				{ -2f, -3f, }, };

		verify(m, eigen.power0(m));
		verify(m, eigen.power1(m));
	}

	@Test
	public void test3() {
		var m = new float[][] { //
				{ -4f, 14f, 0f, }, //
				{ -5f, 13f, 0f, }, //
				{ -1f, 0f, 2f, }, };

		verify(m, eigen.power0(m));
		verify(m, eigen.power1(m));
	}

	@Test
	public void testDegenerate() {
		var m = new float[][] { //
				{ 3f, 2f, 6f, }, //
				{ 2f, 2f, 5f, }, //
				{ -2f, -1f, -4f, }, };

		verify(m, eigen.power0(m));
		// verify(m, eigen.power1(m)); // fail
	}

	@Test
	public void testPca() {
		var size = 9;
		var m = To.matrix(size, size, (i, j) -> i);
		var pc = eigen.pca(m);
		for (var f : pc)
			Math_.verifyEquals(f, pc[0]);
	}

	private void verify(float[][] m, List<DblObjPair<float[]>> pairs) {
		for (var pair : pairs) {
			var eigenValue = pair.t0;
			var eigenVector = pair.t1;
			System.out.println("eigen pair = " + To.string(eigenValue) + " :: " + mtx.toString(eigenVector));
			var n0 = norm(eigenVector);
			var n1 = norm(mtx.mul(m, eigenVector));
			vec.verifyEquals(n0, n1, .01f);
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
