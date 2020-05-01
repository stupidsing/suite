package suite.math.linalg;

import suite.math.Math_;
import suite.streamlet.As;

import java.util.Arrays;

import static java.lang.Math.sqrt;
import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

public class Vector {

	public double abs(float[] m) {
		return abs_(m);
	}

	public double absDiff(float[] m, float[] n) {
		return sqrt(dotDiff_(m, n));
	}

	public float[] add(float[] m, float[] n) {
		return addOn(copy(m), n);
	}

	public float[] addOn(float[] m, float[] n) {
		int length = sameLength_(m, n);
		for (var i = 0; i < length; i++)
			m[i] += n[i];
		return m;
	}

	public float[] addScaleOn(float[] m, float[] n, double f) {
		int length = sameLength_(m, n);
		for (var i = 0; i < length; i++)
			m[i] += n[i] * f;
		return m;
	}

	public double convolute(int l, float[] m, float[] n, int pn) {
		return convolute(l, m, 0, n, pn);
	}

	public double convolute(int l, float[] m, int pm, float[] n, int pn) {
		var d = pm + pn - 1;
		return forInt(pm, pm + l).toDouble(As.sum(i -> m[i] * n[d - i]));
	}

	public float[] copyOf(float[] m) {
		return copy(m);
	}

	public double dot(float[] m) {
		return dot_(m);
	}

	public double dot(float[] m, float[] n) {
		return dot_(m, n);
	}

	public double dotDiff(float[] m, float[] n) {
		return dotDiff_(m, n);
	}

	public float[] neg(float[] m) {
		return negOn(copy(m));
	}

	public float[] negOn(float[] m) {
		var length = m.length;
		for (var i = 0; i < length; i++)
			m[i] = -m[i];
		return m;
	}

	public float[] normalize(float[] m) {
		return normalizeOn(copy(m));
	}

	public float[] normalizeOn(float[] m) {
		return scaleOn(m, 1d / abs_(m));
	}

	public float[] of(double... vs0) {
		var vs1 = new float[vs0.length];
		for (var i = 0; i < vs0.length; i++)
			vs1[i] = (float) vs0[i];
		return vs1;
	}

	public float[] of(float... fs) {
		return fs;
	}

	public int sameLength(float[] m, float[] n) {
		return sameLength_(m, n);
	}

	public float[] scale(float[] m, double d) {
		return scaleOn(copy(m), d);
	}

	public float[] scaleOn(float[] m, double d) {
		var length = m.length;
		for (var i = 0; i < length; i++)
			m[i] *= d;
		return m;
	}

	public float[] sub(float[] m, float[] n) {
		return subOn(copy(m), n);
	}

	public float[] subOn(float[] m, float[] n) {
		int length = sameLength_(m, n);
		for (var i = 0; i < length; i++)
			m[i] -= n[i];
		return m;
	}

	public void verifyEquals(float[] m0, float[] m1) {
		verifyEquals(m0, m1, Math_.epsilon);
	}

	public void verifyEquals(float[] m0, float[] m1, float epsilon) {
		int length = sameLength_(m0, m1);
		for (var i = 0; i < length; i++)
			Math_.verifyEquals(m0[i], m1[i], epsilon);
	}

	private double abs_(float[] m) {
		return sqrt(dot_(m));
	}

	private double dot_(float[] m) {
		return dot_(m, m);
	}

	private double dot_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		var sum = 0d;
		for (var i = 0; i < length; i++)
			sum += m[i] * n[i];
		return sum;
	}

	private double dotDiff_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		var sum = 0d;
		for (var i = 0; i < length; i++) {
			var d = m[i] - n[i];
			sum += d * d;
		}
		return sum;
	}

	private float[] copy(float[] m) {
		return Arrays.copyOf(m, m.length);
	}

	private int sameLength_(float[] m, float[] n) {
		var size = m.length;
		if (size == n.length)
			return size;
		else
			return fail("wrong input sizes");
	}

}
