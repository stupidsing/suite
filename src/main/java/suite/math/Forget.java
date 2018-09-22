package suite.math;

import suite.math.linalg.Vector;

public class Forget {

	private static Vector vec = new Vector();

	public static float[] forget(float[] fs, float[] n) {
		return forgetOn(vec.copyOf(fs), n);
	}

	public static float[] forgetOn(float[] m, float[] n) {
		var length = vec.sameLength(m, n);
		for (var i = 0; i < length; i++)
			m[i] *= n[i];
		return m;
	}

}
