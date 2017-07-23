package suite.math;

import suite.math.linalg.Matrix;

public class Forget {

	private static Matrix mtx = new Matrix();

	public static float[] forget(float[] fs, float[] n) {
		return forgetOn(mtx.of(fs), n);
	}

	public static float[] forgetOn(float[] m, float[] n) {
		int length = mtx.sameLength(m, n);
		for (int i = 0; i < length; i++)
			m[i] *= n[i];
		return m;
	}

}
