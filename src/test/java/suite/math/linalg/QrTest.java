package suite.math.linalg;

import org.junit.Test;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import suite.math.Math_;

public class QrTest {

	private Matrix mtx = new Matrix();

	@Test
	public void test() {
		var qr = new Qr();
		test(qr::decompose);
		test(qr::decomposeByGivensRotation);
	}

	private void test(Fun<float[][], Pair<float[][], float[][]>> fun) {
		float[][] m0 = { //
				{ 12f, -51f, 4f, }, //
				{ 6f, 167f, -68f, }, //
				{ -4f, 24f, -41f, }, //
		};

		var length = mtx.height(m0);
		var qr = fun.apply(m0);

		@SuppressWarnings("unused")
		float[][] expectedq = { //
				{ 6f / 7f, -69f / 175f, -58f / 175f, }, //
				{ 3f / 7f, 158f / 175f, 6f / 175f, }, //
				{ -2f / 7f, 6f / 35f, -33f / 35f, }, //
		};

		@SuppressWarnings("unused")
		float[][] expectedr = { //
				{ 14f, 21f, -14f, }, //
				{ 0f, 175f, -70f, }, //
				{ 0f, 0f, 35f, }, //
		};

		qr.map((q, r) -> {
			System.out.println(mtx.toString(q));
			System.out.println(mtx.toString(r));
			var m1 = mtx.mul(q, r);

			// verify m = QR
			mtx.verifyEquals(m0, m1);

			// verify Q is orthogonal
			mtx.verifyEquals(mtx.mul(q, mtx.transpose(q)), mtx.identity(length));

			// verify R is upper-triangular
			for (var i = 0; i < length; i++)
				for (var j = 0; j < i; j++)
					Math_.verifyEquals(r[i][j], 0f);

			return true;
		});
	}

}
