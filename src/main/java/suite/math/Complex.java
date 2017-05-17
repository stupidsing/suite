package suite.math;

import suite.util.Object_;

public class Complex {

	public final float r, i;

	public static Complex add(Complex u, Complex v) {
		return of(u.r + v.r, u.i + v.i);
	}

	public static Complex sub(Complex u, Complex v) {
		return of(u.r - v.r, u.i - v.i);
	}

	public static Complex mul(Complex u, Complex v) {
		return of(u.r * v.r - u.i * v.i, u.r * v.i + u.i * v.r);
	}

	public static Complex of(float r, float i) {
		return new Complex(r, i);
	}

	public static void verifyEquals(Complex u, Complex v) {
		MathUtil.verifyEquals(u.r, v.r);
		MathUtil.verifyEquals(u.i, v.i);
	}

	private Complex(float r, float i) {
		this.r = r;
		this.i = i;
	}

	public float abs2() {
		return abs2_();
	}

	public Complex conjugate() {
		return Complex.of(r, -i);
	}

	public Complex inverse() {
		float iabs2 = 1f / abs2_();
		return of(r * iabs2, -i * iabs2);
	}

	public Complex scale(float v) {
		return of(r * v, i * v);
	}

	private float abs2_() {
		return r * r + i * i;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Complex.class) {
			Complex other = (Complex) object;
			return r == other.r && i == other.i;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(r) * 31 + Float.hashCode(i);
	}

	@Override
	public String toString() {
		return r + " + " + i + " i";
	}

}
