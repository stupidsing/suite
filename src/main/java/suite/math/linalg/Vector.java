package suite.math.linalg;

import java.util.Arrays;

import suite.math.MathUtil;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.util.Fail;

public class Vector {

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

	public float[] addScaleOn(float[] m, float[] n, double f) {
		int length = sameLength_(m, n);
		for (int i = 0; i < length; i++)
			m[i] += n[i] * f;
		return m;
	}

	public double convolute(int l, float[] m, float[] n, int pn) {
		return convolute(l, m, 0, n, pn);
	}

	public double convolute(int l, float[] m, int pm, float[] n, int pn) {
		var d = pm + pn - 1;
		return Ints_.range(pm, pm + l).toDouble(Int_Dbl.sum(i -> m[i] * n[d - i]));
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
		for (int i = 0; i < length; i++)
			m[i] = -m[i];
		return m;
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
		var length = m.length;
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

	private double dot_(float[] m) {
		return dot_(m, m);
	}

	private double dot_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		double sum = 0d;
		for (int i = 0; i < length; i++)
			sum += m[i] * n[i];
		return sum;
	}

	private double dotDiff_(float[] m, float[] n) {
		int length = sameLength_(m, n);
		double sum = 0d;
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
		var size = m.length;
		if (size == n.length)
			return size;
		else
			return Fail.t("wrong input sizes");
	}

}
