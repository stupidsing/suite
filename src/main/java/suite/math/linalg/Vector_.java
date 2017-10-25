package suite.math.linalg;

import java.util.Arrays;

import suite.math.MathUtil;

public class Vector_ {

	public double abs(float[] m) {
		return abs_(m);
	}

	public double absDiff(float[] m, float[] n) {
		return Math.sqrt(dotDiff_(m, n));
	}

	public float[] add(float[] m, float[] n) {
		return addOn(copy(m), n);
	}

	public float[] addOn(float[] m, float[] n) {
		int length = sameLength_(m, n);
		for (int i = 0; i < length; i++)
			m[i] += n[i];
		return m;
	}

	public float[] addScaleOn(float[] m, float[] n, float f) {
		int length = sameLength_(m, n);
		for (int i = 0; i < length; i++)
			m[i] += n[i] * f;
		return m;
	}

	public float dot(float[] m) {
		return dot_(m);
	}

	public float dot(float[] m, float[] n) {
		return dot_(m, n);
	}

	public float dotDiff(float[] m, float[] n) {
		return dotDiff_(m, n);
	}

	public float[] normalize(float[] m) {
		return scale(m, 1d / abs_(m));
	}

	public float[] of(float[] m) {
		return copy(m);
	}

	public int sameLength(float[] m, float[] n) {
		return sameLength_(m, n);
	}

	public float[] scale(float[] m, double d) {
		return scaleOn(copy(m), d);
	}

	public float[] scaleOn(float[] m, double d) {
		int length = m.length;
		for (int i = 0; i < length; i++)
			m[i] *= d;
		return m;
	}

	public float[] sub(float[] m, float[] n) {
		return subOn(copy(m), n);
	}

	public float[] subOn(float[] m, float[] n) {
		int length = sameLength_(m, n);
		for (int i = 0; i < length; i++)
			m[i] -= n[i];
		return m;
	}

	public void verifyEquals(float[] m0, float[] m1) {
		verifyEquals(m0, m1, MathUtil.epsilon);
	}

	public void verifyEquals(float[] m0, float[] m1, float epsilon) {
		int length = sameLength_(m0, m1);
		for (int i = 0; i < length; i++)
			MathUtil.verifyEquals(m0[i], m1[i], epsilon);
	}

	private double abs_(float[] m) {
		return Math.sqrt(dot_(m));
	}

	private float dot_(float[] m) {
		return dot_(m, m);
	}

	private float dot_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		float sum = 0;
		for (int i = 0; i < length; i++)
			sum += m[i] * n[i];
		return sum;
	}

	private float dotDiff_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		float sum = 0;
		for (int i = 0; i < length; i++) {
			double d = m[i] - n[i];
			sum += d * d;
		}
		return sum;
	}

	private float[] copy(float[] m) {
		return Arrays.copyOf(m, m.length);
	}

	private int sameLength_(float[] m, float[] n) {
		int size = m.length;
		if (size == n.length)
			return size;
		else
			throw new RuntimeException("wrong input sizes");
	}

}
