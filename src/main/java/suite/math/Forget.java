package suite.math;

import suite.math.linalg.Matrix_;

public class Forget {

	private static Matrix_ mtx = new Matrix_();

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
