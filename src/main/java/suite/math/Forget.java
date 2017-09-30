package suite.math;

import suite.math.linalg.Vector_;

public class Forget {

	private static Vector_ vec = new Vector_();

	public static float[] forget(float[] fs, float[] n) {
		return forgetOn(vec.of(fs), n);
	}

	public static float[] forgetOn(float[] m, float[] n) {
		int length = vec.sameLength(m, n);
		for (int i = 0; i < length; i++)
			m[i] *= n[i];
		return m;
	}

}
