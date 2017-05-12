package suite.math;

public class Forget {

	private static Matrix mtx = new Matrix();

	public static float[] forget(float[] fs, float[] n) {
		return forgetOn(mtx.of(fs), n);
	}

	public static float[] forgetOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] *= n[i];
		else
			throw new RuntimeException("wrong matrix sizes");
		return m;
	}

}
