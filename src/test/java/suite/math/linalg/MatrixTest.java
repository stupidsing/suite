package suite.math.linalg;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import suite.math.Math_;
import suite.util.To;

public class MatrixTest {

	private Matrix mtx = new Matrix();

	@Test
	public void testDet0() {
		float[][] m = { //
				{ 2f, 0f, 0f, }, //
				{ 0f, 3f, 0f, }, //
				{ 0f, 0f, 4f, }, };
		Math_.verifyEquals(24f, mtx.det(m));
	}

	@Test
	public void testDet1() {
		float[][] m = { //
				{ -2f, 2f, -3f, }, //
				{ -1f, 1f, 3f, }, //
				{ 2f, 0f, -1f, }, };
		Math_.verifyEquals(18f, mtx.det(m));
	}

	@Test
	public void testInverse() {
		var id = mtx.identity(3);
		assertTrue(mtx.equals(id, mtx.inverse(id)));

		var mul8 = mtx.scale(id, 8f);
		var div8 = mtx.scale(id, 1f / 8f);
		assertTrue(mtx.equals(mul8, mtx.inverse(div8)));
		assertTrue(mtx.equals(div8, mtx.inverse(mul8)));

		var o = new float[2][2];
		o[0][1] = 1;
		o[1][0] = 1;
		assertTrue(mtx.equals(o, mtx.inverse(o)));
	}

	@Test
	public void testInversePerformance() {
		var random = new Random();
		var large0 = To.matrix(128, 128, (i, j) -> random.nextFloat());
		var large1 = mtx.inverse(large0);
		var actual = mtx.mul(large0, large1);
		var expect = mtx.identity(128);
		mtx.verifyEquals(expect, actual, .05f);
	}

}
