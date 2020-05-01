package suite.math.linalg;

import org.junit.jupiter.api.Test;
import primal.primitive.adt.pair.DblObjPair;
import suite.math.Math_;
import suite.util.To;

import java.util.List;

public class EigenTest {

	private float epsilon = .01f;
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

		verify(m, eigen.power0(m)); // actually fail, duplicated eigen vector
		// verify(m, eigen.power1(m)); // fail
	}

	@Test
	public void testPca0() {
		var size = 9;
		var m = To.matrix(size, size, (i, j) -> i);
		var pc = eigen.new Pca(m, size).eigenVectors[0];
		for (var f : pc)
			Math_.verifyEquals(f, pc[0]);
	}

	// http://www.iro.umontreal.ca/~pift6080/H09/documents/papers/pca_tutorial.pdf
	@Test
	public void testPca1() {
		var m = new float[][] { //
				{ 2.5f, 2.4f, }, //
				{ .5f, .7f, }, //
				{ 2.2f, 2.9f, }, //
				{ 1.9f, 2.2f, }, //
				{ 3.1f, 3f, }, //
				{ 2.3f, 2.7f, }, //
				{ 2f, 1.6f, }, //
				{ 1f, 1.1f, }, //
				{ 1.5f, 1.6f, }, //
				{ 1.1f, .9f, }, };

		var p1 = eigen.new Pca(m, 1);
		var p2 = eigen.new Pca(m, 2);
		var eigenVector0 = p2.eigenVectors[0];
		// vec.normalize(pc) = { .6779f, .7352f, }
		var pca0 = p1.pca;
		var pca1 = p2.pca;
		System.out.println(mtx.toString(pca0));
		System.out.println(mtx.toString(pca1));
		Math_.verifyEquals(.9221f, eigenVector0[0] / eigenVector0[1], epsilon);
		Math_.verifyEquals(.8280f, pca0[0][0], epsilon);
		Math_.verifyEquals(.8280f, pca1[0][0], epsilon);
		Math_.verifyEquals(.1751f, pca1[0][1], epsilon);
		Math_.verifyEquals(-1.2238f, pca0[9][0], epsilon);
		Math_.verifyEquals(-1.2238f, pca1[9][0], epsilon);
		Math_.verifyEquals(.1627f, pca1[9][1], epsilon);
	}

	private void verify(float[][] m, List<DblObjPair<float[]>> pairs) {
		for (var pair : pairs) {
			var eigenValue = pair.k;
			var eigenVector = pair.v;
			System.out.println("eigen pair = " + To.string(eigenValue) + " :: " + mtx.toString(eigenVector));
			var n0 = vec.scale(eigenVector, eigenValue);
			var n1 = mtx.mul(m, eigenVector);
			vec.verifyEquals(n0, n1, epsilon);
		}
	}

}
