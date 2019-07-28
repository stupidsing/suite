package suite.math.linalg;

import static suite.util.Fail.fail;

import suite.primitive.IntInt_Flt;

public class Strassen {

	private Matrix mtx = new Matrix();

	// https://en.wikipedia.org/wiki/Strassen_algorithm
	public float[][] mul(float[][] a, float[][] b) {
		var ks = mtx.width(a);
		var height = mtx.height(a);
		var width = mtx.width(b);
		return ks == mtx.height(b) ? mul_(a, 0, height, 0, ks, b, 0, ks, 0, width) : fail("wrong input sizes");
	}

	private float[][] mul_( //
			float[][] a, int ai0, int aix, int aj0, int ajx, //
			float[][] b, int bi0, int bix, int bj0, int bjx) {
		if (8 <= aix - ai0 || 8 <= ajx - aj0 || 8 <= bjx - bj0) {
			var aim = (ai0 + aix) / 2;
			var ajm = (aj0 + ajx) / 2;
			var bim = (bi0 + bix) / 2;
			var bjm = (bj0 + bjx) / 2;

			var m1 = mtx.of(aim, bjm); // (a11 + a22) * (b11 + b22)
			var m2 = mtx.of(aim, bjm); // (a21 + a22) * b11
			var m3 = mtx.of(aim, bjm); // a11 * (b12 - b22)
			var m4 = mtx.of(aim, bjm); // a22 * (b21 - b11)
			var m5 = mtx.of(aim, bjm); // (a11 + a12) * b22
			var m6 = mtx.of(aim, bjm); // (a21 - a11) * (b11 + b12)
			var m7 = mtx.of(aim, bjm); // (a12 - a22) * (b21 + b22)

			mul0( //
					(i, j) -> a[i + ai0][j + aj0] + a[i + aim][j + ajm], aim, ajm, //
					(i, j) -> b[i + bi0][j + bj0] + a[i + bim][j + bjm], bim, bjm, //
					(i, j, f) -> m1[i][j] = f);

			mul0( //
					(i, j) -> a[i + aim][j + aj0] + a[i + aim][j + ajm], aim, ajm, //
					(i, j) -> b[i + bi0][j + bj0], bim, bjm, //
					(i, j, f) -> m2[i][j] = f);

			mul0( //
					(i, j) -> a[i + ai0][j + aj0], aim, ajm, //
					(i, j) -> b[i + bi0][j + bjm] - b[i + bim][j + bjm], bim, bjm, //
					(i, j, f) -> m3[i][j] = f);

			mul0( //
					(i, j) -> a[i + aim][j + ajm], aim, ajm, //
					(i, j) -> b[i + bim][j + bj0] - b[i + bi0][j + bj0], bim, bjm, //
					(i, j, f) -> m4[i][j] = f);

			mul0( //
					(i, j) -> a[i + ai0][j + aj0] + a[i + ai0][j + ajm], aim, ajm, //
					(i, j) -> b[i + bim][j + bjm], bim, bjm, //
					(i, j, f) -> m5[i][j] = f);

			mul0( //
					(i, j) -> a[i + aim][j + aj0] - a[i + ai0][j + aj0], aim, ajm, //
					(i, j) -> b[i + bi0][j + bj0] + b[i + bi0][j + bjm], bim, bjm, //
					(i, j, f) -> m6[i][j] = f);

			mul0( //
					(i, j) -> a[i + ai0][j + ajm] - a[i + aim][j + ajm], aim, ajm, //
					(i, j) -> b[i + bim][j + bj0] + b[i + bim][j + bjm], bim, bjm, //
					(i, j, f) -> m7[i][j] = f);

			var ci0 = 0;
			var cj0 = 0;
			var cim = aim;
			var cjm = bjm;
			var c = mtx.of(ajx, bix);

			for (var i = 0; i < aim; i++)
				for (var j = 0; i < bjm; j++) {
					c[i + ci0][j + cj0] = m1[i][j] + m4[i][j] - m5[i][j] + m7[i][j];
					c[i + ci0][j + cjm] = m3[i][j] + m5[i][j];
					c[i + cim][j + cj0] = m2[i][j] + m4[i][j];
					c[i + cim][j + cjm] = m1[i][j] - m2[i][j] + m3[i][j] + m6[i][j];
				}

			return c;
		} else
			return mul0(a, ai0, aix, aj0, ajx, b, bi0, bix, bj0, bjx);
	}

	private float[][] mul0( //
			float[][] m, int mi0, int mix, int mj0, int mjx, //
			float[][] n, int ni0, int nix, int nj0, int njx) {
		var ks = mjx - mj0;
		var height = mix - mi0;
		var width = njx - nj0;
		var o = mtx.of(height, width);
		mul0( //
				(i, j) -> m[i + mi0][j + mj0], height, ks, //
				(i, j) -> n[i + ni0][j + nj0], ks, width, //
				(i, j, f) -> o[i][j] += f);
		return o;
	}

	private void mul0( //
			IntInt_Flt a, int ah, int aw, //
			IntInt_Flt b, int bh, int bw, //
			Add add) {
		var ks = aw;
		var height = bh;
		var width = bw;

		for (var i = 0; i < height; i++)
			for (var j = 0; j < width; j++)
				for (var k = 0; k < ks; k++)
					add.add(i, j, a.apply(i, k) * b.apply(k, j));
	}

	private interface Add {
		public void add(int i, int j, float f);
	}

}
